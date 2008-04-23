/*
  This file is part of the NextText project.
  http://www.nexttext.net/

  Copyright (c) 2004-08 Obx Labs / Jason Lewis

  NextText is free software: you can redistribute it and/or modify it under
  the terms of the GNU General Public License as published by the Free Software 
  Foundation, either version 2 of the License, or (at your option) any later 
  version.

  NextText is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR 
  A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along with 
  NextText.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.nexttext.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import net.nexttext.TextObjectGlyph;
import net.nexttext.TextObjectGlyphIterator;
import net.nexttext.TextObjectGroup;
import net.nexttext.Vector3;
import net.nexttext.property.ColorProperty;
import net.nexttext.renderer.Java2DTextPageRenderer;

/**
 * This renderer callback allows for the replacement of textObjects with rasters
 * (static bitmap representations) of their appearance at the time rasterize is
 * called.
 * 
 * <p>
 * The object maintains an image to which the rasters can be drawn to. The image
 * has an alpha channel.
 * </p>
 * 
 */
/* $Id$ */
public class TextObjectRasterizer extends Java2DTextPageRenderer {

    // bufferedImage object to hold the rasterized text
    BufferedImage raster;

    public TextObjectRasterizer(Component canvas, Dimension imageSize) {
        super(canvas, null);
        raster = new BufferedImage((int) imageSize.getWidth(), (int) imageSize
                .getHeight(), BufferedImage.TYPE_INT_ARGB);
        g2 = raster.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     * This method draws the textObject to the image held by this rasterizer
     * 
     * <p>
     * The TextObject passed into this method is also made completely
     * transparent by this method. This is to avoid a possible change in
     * appearance that would result from drawing the original object over the
     * raster
     * </p>
     */
    public void rasterize(TextObjectGroup text) {
        // Store the original transform
        AffineTransform at = g2.getTransform();
        // Store the original font
        Font oldFont = g2.getFont();

        // Draw the textObject
        TextObjectGlyphIterator iterator = text.glyphIterator();
        while (iterator.hasNext()) {
            TextObjectGlyph glyph = iterator.next();

            Vector3 pos = glyph.getPositionAbsolute();
            g2.translate(pos.x, pos.y);
            g2.rotate(glyph.getRotation().get());
            // Ask the renderer to actually draw the glyph to
            // this objects graphics context (the image).
            renderGlyph(glyph);

            // restore the original transform and font
            g2.setTransform(at);
            g2.setFont(oldFont);
        }

        /*
         * Fix the scintillations, make the object transparent before drawing
         * the raster. If this is not done the result is that the text appears
         * to be brighter for an instant as the raster is rendered on top of the
         * actual textObject.
         * 
         */
        ColorProperty colProp = text.getColor();
        Color c = colProp.get();
        Color newColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0);
        colProp.set(newColor);

        ColorProperty stroke = text.getStrokeColor();
        Color s = stroke.get();
        Color newSColor = new Color(s.getRed(), s.getGreen(), s.getBlue(), 0);
        stroke.set(newSColor);
    }

    /**
     * Clear all the text.
     * 
     * <p>
     * Resets all the images pixels to a transparent colour.
     * </p>
     */
    public void clear() {
        WritableRaster imgRaster = raster.getRaster();
        int numPixels = imgRaster.getWidth() * imgRaster.getHeight();
        int[] rgb = new int[numPixels];

        for (int i = 0; i < numPixels; i++) {
            rgb[i] = 0x00FFFFFF;
        }
        imgRaster.setDataElements(0, 0, imgRaster.getWidth(), imgRaster
                .getHeight(), rgb);
    }
}