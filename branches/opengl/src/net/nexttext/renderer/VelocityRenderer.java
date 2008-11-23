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

import net.nexttext.*;
import net.nexttext.property.*;
import processing.core.*;

/**
 * Traverses the TextObject hierarchy and draws every object's velocity as a
 * line starting from the object's position. This callback is useful to debug
 * behaviours that make use of the Velocity property.
 */
/* $Id$ */
public class VelocityRenderer extends TextPageRenderer {
    private int color;
    private int scale;

    /**
     * Builds a VelocityRenderer.
     * 
     * @param p the parent PApplet
     * @param color the color that will be used to render the vector
     * @param scale a scalar that will be used to make the vector proportionally
     *  larger. This is useful since (x,y) values for velocity are often in the
     *  range 0~5.
     */
    public VelocityRenderer(PApplet p, int color, int scale) {
        super(p);
        this.color = color;
        this.scale = scale;
    }
    
    /**
     * Builds a VelocityRenderer.
     * 
     * @param p the parent PApplet
     * @param r the red value of the color that will be used to render the vector
     * @param g the green value of the color that will be used to render the vector
     * @param b the blue value of the color that will be used to render the vector
     * @param scale a scalar that will be used to make the vector proportionally
     *  larger. This is useful since (x,y) values for velocity are often in the
     *  range 0~5.
     */
    public VelocityRenderer(PApplet p, int r, int g, int b, int scale) {
        this(p, p.color(r, g, b), scale);
    }

    /**
     * Traverses the TextObject hierarchy and renders a velocity vector for any
     * TextObject having a Velocity property.
     * 
     * @param textPage the TextPage to render
     */
    public void renderPage(TextPage textPage) {
        TextObjectIterator toi = textPage.getTextRoot().iterator();

        while (toi.hasNext()) {
            TextObject to = toi.next();

            Vector3Property velProp = (Vector3Property) to
                    .getProperty("Velocity");

            if (velProp != null) {
                Vector3 vel = velProp.get();
                Vector3 pos = to.getPositionAbsolute();
                vel.scalar(scale);
                
                // save the color properties
                boolean stroke = p.g.stroke;
                int strokeColor = p.g.strokeColor;
                boolean fill = p.g.fill;
                int fillColor = p.g.fillColor;
                
                // draw the line
                p.stroke(color);
                p.noFill();
                p.line((float)pos.x, (float)pos.y, (float)(pos.x + vel.x), (float)(pos.y + vel.y));
                
                // restore saved properties
                if (stroke) 
                    p.stroke(strokeColor);
                else 
                    p.noStroke();

                if (fill) 
                    p.fill(fillColor);
                else 
                    p.noFill();
            }
        }
    }
}
