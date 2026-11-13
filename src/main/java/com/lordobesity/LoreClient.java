package com.lordobesity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Solo cliente: abre el visor de libros con el texto de assets/lordobesity/lore/*.txt.
 * Las paginas se separan con una linea que solo contenga "---". Se puede editar el texto sin tocar el codigo.
 */
public class LoreClient {
    private static List<String> readPages(String name) {
        List<String> pages = new ArrayList<>();
        try {
            var res = Minecraft.getInstance().getResourceManager()
                    .getResource(new ResourceLocation(LordObesityMod.MODID, "lore/" + name + ".txt"));
            if (res.isPresent()) {
                try (BufferedReader r = res.get().openAsReader()) {
                    StringBuilder cur = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (line.trim().equals("---")) {
                            pages.add(cur.toString().trim());
                            cur.setLength(0);
                        } else {
                            if (cur.length() > 0) cur.append('\n');
                            cur.append(line);
                        }
                    }
                    if (cur.toString().trim().length() > 0) pages.add(cur.toString().trim());
                }
            }
        } catch (IOException ignored) {
        }
        if (pages.isEmpty()) pages.add("(Las páginas están en blanco.)");
        return pages;
    }

    public static void open(int volume) {
        List<String> pages = new ArrayList<>();
        if (volume >= 1 && volume <= 3) {
            pages.addAll(readPages("book" + volume));
        } else {                                   // libro completo: los tres + epilogo
            for (int i = 1; i <= 3; i++) pages.addAll(readPages("book" + i));
            pages.addAll(readPages("epilogue"));
        }
        final List<String> fp = pages;
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess() {
            @Override
            public int getPageCount() {
                return fp.size();
            }

            @Override
            public FormattedText getPageRaw(int i) {
                return FormattedText.of(fp.get(i));
            }
        }));
    }
}
