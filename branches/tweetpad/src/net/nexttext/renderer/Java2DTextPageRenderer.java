package net.nexttext.renderer;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.util.Iterator;
import java.util.Stack;

import net.nexttext.TextObject;
import net.nexttext.TextObjectGlyph;
import net.nexttext.TextObjectGroup;
import net.nexttext.TextPage;
import net.nexttext.Vector3;
import net.nexttext.property.Vector3Property;
import net.nexttext.property.Vector3PropertyList;

/**
 * 
 * Renders the text stored in a text page.
 * 
 * <p>This TextPage renderer is based on the Java2D api</p>
 *
 */
public class Java2DTextPageRenderer implements TextPageRenderer{

        /** 
         *
         * The rendering loop.  Takes as input a TextPage and traverses its root
         * node, rendering all the TextObjectGlyph objects along the way.
         *
         */
        public void render( TextPage textPage, Graphics2D g2, Component c) {
            // When resizing, it's possible to loose the reference to the graphics
            // context, so we skip rendering the frame.            
            if (g2==null){
                System.out.println(("Skip rendering frame because the graphics " +
                "context was lost temporarily."));
                return;
            }      

            // traverse the TextObject hierarchy
            if (textPage.getTextRoot() == null) {
                System.out.println("Java2DTextPage: No root specified yet");
            }
            else {
                traverse(textPage.getTextRoot(), g2);
            }
        } // end rendering

        /**
         * Traverse the TextObject tree and render all of its glyphs.
         */
        private void traverse(TextObject root, Graphics2D g2) {
            // The tree is traversed using a variable to point at the current node
            // being processed.  TextObjects specify their rotation and position
            // relative to their parent, which is handled by registering coordinate
            // system changes with the Graphics2D object as the tree is traversed.

            // Currently rendering is not synchronized with modifications to the
            // TextObjectTree.  This is dodgy, but gives a performance boost, so
            // will stay that way for the moment.  However, it affects this method,
            // because we can't assume that the tree is always well structured.

            // Transformations are stored in a stack so that they can be undone as
            // needed.  It is not appropriate to use the position of the TextObject
            // to undo the transformation, because this may have changed due to the
            // lack of synchronization.

            TextObject current = root;
            Stack coordTransforms = new Stack();
            do {
                // Draw any glyphs.
                if (current instanceof TextObjectGlyph) {
                    enterCoords(coordTransforms, current, g2);
                    renderGlyph((TextObjectGlyph)current, g2);
                    exitCoords(coordTransforms, g2);
                }

                // Descend to process any children
                if (current instanceof TextObjectGroup) {
                    TextObjectGroup tog = (TextObjectGroup) current;
                    TextObject child = tog.getLeftMostChild();
                    if (child != null) {
                        enterCoords(coordTransforms, current, g2);
                        current = child;
                        continue;
                    }
                }

                // Processing of this node is complete, so move on to siblings.
                // Since a node may not have siblings, a search is made up the tree
                // for the first appropriate sibling.  The search ends if a sibling
                // is found, or if it reaches the top of the tree.
                while (current != root) {
                    TextObject sibling = current.getRightSibling();
                    if (sibling != null) {
                        current = sibling;
                        break;
                    } else {
                        current = current.getParent();
                        if (current == null) {
                            // Aaarghh, we were detached from the tree mid-render,
                            // so just abort the whole process.
                            return;
                        }
                        exitCoords(coordTransforms, g2);
                    }
                }
            } while (current != root);
        }

        /**
         * Transform the Graphics2D into the coordinates of the given TextObject.
         *
         * <p>Once this transformation is done, the TextObject and any of it's
         * children can be written directly to the Graphics2D without having to
         * handle position or rotation.  </p>
         */
        private void enterCoords(Stack ct, TextObject node, Graphics2D g2) {
        	Vector3 pos = node.getPosition().get();
            g2.translate(pos.x, pos.y);
            ct.push(pos);
    	    
            double rotation = node.getRotation().get();
            g2.rotate(rotation);
            ct.push(new Double(rotation));
        }

        /**
         * Transform the Graphics2D out of the coordinates on top of the stack.
         *
         * <p>This undoes the change of enterCoords().  </p>
         */
        private void exitCoords(Stack ct, Graphics2D g2) {
        	double rotation = ((Double)ct.pop()).doubleValue();
            g2.rotate(-rotation);

            Vector3 pos = (Vector3) ct.pop();
            g2.translate(-pos.x, -pos.y);
        }

        /**
         * Renders a TextObjectGlyph using quads, either as an outline or as a
         * filled shape.
         *
         * @param glyph  The TextObjectGlyph
         * @param g2     The drawing context
         */
        public void renderGlyph( TextObjectGlyph glyph, Graphics2D g2 ) {

            //////////////////////////////////////
            // Optmize based on presence of DForms and of outlines
            if ( glyph.isDeformed() || glyph.isStroked() ) {

                //////////////////////////////////
                // Render glyph using vertex list

                // Use the cached path if possible.
                GeneralPath gp = (GeneralPath)glyph.rendererCache;

                if ( gp == null ) {
                    // we need to rebuild the cache
                    // get the list of vertices for this glyph
                    Vector3PropertyList vertices = (Vector3PropertyList)glyph.getProperty("Control Points");
                    // create a new GeneralPath to hold the vector outline
                    gp = new GeneralPath();
                    // get an iterator for the list of contours
                    Iterator it = glyph.contours.iterator();  

                    // process each contour
                    while( it.hasNext() ) {

                        // get the list of vertices for this contour
                        int contour[] = (int[])it.next();

                        Vector3Property firstPoint = vertices.get(contour[0]);
                        // move the pen to the begining of the contour
                        gp.moveTo((float)firstPoint.getX(),(float)firstPoint.getY());

                        // generate all the quads forming the line
                        for (int i=1; i < contour.length; i++) {        

                            Vector3Property current = vertices.get(contour[i]);
                            Vector3Property next;

                            // Since it's a closed contour, the last vertex's next
                            // is the first vertex.
                            if (i == contour.length - 1)
                                next = vertices.get(contour[0]);
                            else
                                next = vertices.get(contour[i+1]);

                            float anchorx = (float)(current.getX() + next.getX())/2;
                            float anchory = (float)(current.getY() + next.getY())/2;

                            gp.quadTo((float)current.getX(),
                                    (float)current.getY(),
                                    anchorx,
                                    anchory);
                        }   
                        // close the path
                        gp.closePath();
                        // cache it
                        glyph.rendererCache = gp;
                    } // end while  
                }


                // draw the outline of the shape
                if (glyph.isStroked()) {
                    g2.setColor(glyph.getStrokeColorAbsolute());
                    g2.setStroke(glyph.getStrokeAbsolute());
                    g2.draw(gp);
                }

                // fill the shape
                if (glyph.isFilled()) {
                    g2.setColor(glyph.getColorAbsolute());
                    g2.fill(gp);
                }
            }
            else {       
                ///////////////////////////////////////////
                // Render glyph using Graphics.drawString()
                g2.setColor( glyph.getColorAbsolute() );
                // set the font
                g2.setFont( glyph.getFont() );
                // draw the glyph
                g2.drawString( glyph.getGlyph(), 0, 0 );
            }

        } // end renderGlyph

    }