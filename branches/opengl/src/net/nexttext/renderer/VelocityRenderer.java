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

import java.awt.*;
import net.nexttext.*;
import net.nexttext.property.*;

/**
 * Traverses the TextObject hierarchy and draws every object's velocity as a
 * line starting from the object's position. This callback is useful to debug
 * behaviours that make use of the Velocity property.
 */
/* $Id$ */
public class VelocityRenderer extends TextPageRenderer {

    Graphics2D g2;
    Color      color;
    int        scale;

    /**
     * Constructs a RenderVelocity callback. Color is the color that will be
     * used to render the vector. Scale is a scalar that will be used to make
     * the vector proportionally larger. This is useful since in many cases
     * (x,y) values for velocity are often in the range 0~5.
     */

    public VelocityRenderer(Component canvas, Graphics2D g2, Color color,
            int scale) {
        super(canvas);
        this.g2 = g2;
        this.color = color;
        this.scale = scale;
    }

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

                g2.setColor(color);
                g2.drawLine((int) pos.x, (int) pos.y, (int) (pos.x + vel.x),
                        (int) (pos.y + vel.y));
            }
        }
    }
}
