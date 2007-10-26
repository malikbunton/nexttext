package net.nexttext.processing;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import processing.core.PApplet;

/**
 * This class controls access to custom fonts in the 'data' folder of the sketch. The 
 * fonts must be in the TrueType format with extension '.ttf'.
 */
public class FontManager {

    static final String REVISION = "$Header: /Volumes/Storage/Data/Groups/obx/CVS/NextText/src/net/nexttext/processing/Attic/FontManager.java,v 1.1.2.1 2007/09/22 16:06:22 elie Exp $";
    
    static final float DEFAULT_SIZE = 48.0f;
    
    /**
     * Loads the font with the given filename from the 'data' folder of the sketch
     * 
     * @param the parent PApplet
     * @param filename
     * 
     * @return the loaded font
     */
    public static Font loadFont(PApplet pApplet, String filename) {
    	try {
    		return Font.createFont(Font.TRUETYPE_FONT, pApplet.openStream(filename)).deriveFont(DEFAULT_SIZE);
    	} catch (IOException ioe) {
    		PApplet.println("ERROR loading font '"+filename+"'! Make sure it is in the 'data' folder of your sketch and that you typed the filename correctly.");
    		ioe.printStackTrace();
    	} catch (FontFormatException ffe) {
    		PApplet.println("ERROR loading font '"+filename+"'! There appears to be something wrong with the font file...");
    		ffe.printStackTrace();
    	}
    	
    	return null;
    }
    
    /**
     * Derives a font with the given size from the given font.
     * 
     * @param font font to derive
     * @param size font size
     *
     * @return the derived font
     */
    public static Font deriveFont(Font font, float size) {
    	return font.deriveFont(size);
    }
    
    /**
     * Derives a font with the supplied attributes from the given font.
     * 
     * @param font font to derive
     * @param size font size
     * @param italic whether the font is italic
     * @param bold whether the font is bold
     *
     * @return the derived font
     */
    public static Font deriveFont(Font font, float size, boolean italic, boolean bold) {
        int style = Font.PLAIN;
        if (bold) 
            style += Font.BOLD;
        if (italic) 
            style += Font.ITALIC;
        
        return font.deriveFont(style, size);
    }
}
