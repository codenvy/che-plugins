package org.eclipse.search.internal.ui;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import java.io.IOException;

/**
 * @author Evgen Vidolob
 */
public class SearchPlugin {
    public static void log(IOException e) {
        JavaPlugin.log(e);
    }
}
