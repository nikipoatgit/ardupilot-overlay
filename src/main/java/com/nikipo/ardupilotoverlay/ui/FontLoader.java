package com.nikipo.ardupilotoverlay.ui;

import javafx.scene.text.Font;

import java.util.Objects;

public final class FontLoader {

    private FontLoader() {}

    public static void loadFonts() {

        load("/fonts/NotoSans-Regular.ttf");
        load("/fonts/NotoSans-Bold.ttf");
        load("/fonts/NotoSansMono-Regular.ttf");

    }

    private static void load(String path) {

        Font.loadFont(
                Objects.requireNonNull(
                        FontLoader.class.getResourceAsStream(path)
                ),
                12
        );

    }
}