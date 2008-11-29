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

package net.nexttext;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.nexttext.behaviour.*;
import net.nexttext.input.*;
import net.nexttext.renderer.*;
import net.nexttext.property.ColorProperty;
import net.nexttext.property.StrokeProperty;

import processing.core.*;

/**
 * The container for the NextText for Processing data and window.
 * <p>The PApplet which uses NextText uses this as its primary point of
 * entry into the NextText data.  Most importantly, it contains the TextObjects
 * which hold the core text data, and the TextRenderer and window to actually
 * draw the stuff.  </p>
 * <p>The Book has its own step(), draw(), and stepAndDraw() methods which replace
 * the Simulator loop found in regular NextText applications.</p>
 * <p>Any updates to the TextObject tree must be synchronized on the Book. </p>
 */
 /* $Id$ */
public class Book {

	public static TextObjectBuilder toBuilder;
    public static MouseDefault mouse;
    public static KeyboardDefault keyboard;
    
    private PApplet p;   

    /**
     * The current frame count.
     */
    long frameCount = 0;
	
    /** The frame number, incremented each frame by the Simulator. */
    public long getFrameCount() { return frameCount; }

    /** The frame number, incremented each frame by the Simulator. */
    public void incrementFrameCount() { frameCount++; }
	
    protected LinkedHashMap<String, TextPage> pages;
    protected PGraphicsTextPageRenderer defaultRenderer;
    protected List<AbstractBehaviour> behaviourList;
    protected TextObjectRoot textRoot;	// the root of the TextObject hierarchy
    protected InputManager inputs;
    protected SpatialList spatialList;
    
    
    /**
     * Instantiates the Book.
     * 
     * @param p the parent PApplet
     */
    public Book(PApplet p) {
    	defaultRenderer = new PGraphicsTextPageRenderer(p); 
        pages = new LinkedHashMap<String, TextPage>();
    	behaviourList = new LinkedList<AbstractBehaviour>();
    	textRoot = new TextObjectRoot(this);
        spatialList = new SpatialList();
        
        this.p = p;
        
        // create a default text page
        TextPage defaultTextPage = new TextPage(this, defaultRenderer);
        addPage("Default Text Page", defaultTextPage);
        
        // initialize the TextObjectBuilder
        toBuilder = new TextObjectBuilder(this, defaultTextPage);
        
        // initialize the inputs
        mouse = new MouseDefault(p);
        keyboard = new KeyboardDefault(p);
        inputs = new InputManager(mouse, keyboard);
    }
    
	///////////////////////////////////////////////////////////////////////////
	
	private Set<TextObject> objectsToRemove = new HashSet<TextObject>();

    /**
     * Remove a text object and any children from the tree.
     *
     * <p>This method will:</p>
     * <ul><li>Remove the objects from the spatial list</li>
     * <li>Remove the objects from any behaviours</li>
     * <li>Remove the objects from the text object hierarchy</li>
     * </ul>
     *
     * <p>The object is not removed immediately, it is removed prior to
     * starting the next simulator run.  This means that behaviours can safely
     * call this method without worrying that the object will disappear and
     * mess up other behaviours in the chain.  </p>
     */

	public void removeObject(TextObject to) {
	    objectsToRemove.add(to);
	}
	
	/**
	 * Removes all objects that have been marked for deletion.  
	 * 
	 * <p>Do not call this method while iterating over the TextObjectRoot 
	 * for synchronization reasons. </p> 
	 */
	protected void removeQueuedObjects() {
	    
	    if ( objectsToRemove.size() > 0 ) {
	        
	        for ( Iterator<TextObject> i = objectsToRemove.iterator(); i.hasNext(); ) {
                TextObject next = i.next();
                if (next instanceof TextObjectGlyph) {
                    removeObjectInner(next);
                } else if (next instanceof TextObjectGroup) {
                    TextObjectIterator toi = ((TextObjectGroup) next).iterator();
                    while (toi.hasNext()) {
                        removeObjectInner(toi.next());
                    }
                } else {
                    throw new RuntimeException("Unexpected TextObject subtype");
                }
	        }

	        objectsToRemove.clear();
	    }
	}

	/**
	 * See removeObject(TextObject to)
	 */
	private synchronized void removeObjectInner(TextObject to) {
		// remove the object from the spatial list
		getSpatialList().remove( to );
		// traverse the behaviour list.  try to remove the object from each
		// active behaviour
    	Iterator<AbstractBehaviour> i = behaviourList.iterator();
    	while (i.hasNext()) {
    	    AbstractBehaviour b = i.next();
    	    b.removeObject(to);    	        	    					
    	}
		// detach the object from the tree
		to.detach();			
	}
	
	///////////////////////////////////////////////////////////////////////////
	// Step and draw methods 
	
	/**
     * Applies all active behaviours.
     * <p>This code is adapted from Simulator.run().</p>
     */
    public synchronized void step() {
        // apply the behaviours
        Iterator<AbstractBehaviour> i = behaviourList.iterator();
        while (i.hasNext()) {
            AbstractBehaviour b = i.next();
            b.behaveAll(); 
        }

        // remove all objects flagged for deletion 
        removeQueuedObjects();

        // the new frame is calculated, so make it the current frame
        incrementFrameCount();

        // update the spatial list
        spatialList.update();
    }
    
    /**
     * Renders a frame.
     */
    public void draw() {
        // render all the pages
        Collection<TextPage> pages = getPages();
        Iterator<TextPage> i = pages.iterator();
        while (i.hasNext()) {
            TextPage page = i.next();
            page.render();
        }
    }
    
    /**
     * Renders the given page for this frame.
     * 
     * @param pageName the name of the Page to render
     */
    public void drawPage(String pageName) {
        getPage(pageName).render();
    }
    
    /**
     * Goes through a full iteration of the Book loop, i.e. applies all behaviours
     * and then renders a frame.
     */
    public void stepAndDraw() {
    	step();
    	draw();
    }
    
    ///////////////////////////////////////////////////////////////////////////
	// Font loading and setting methods 
	
    
    /**
     * Sets the font of the TextObjectBuilder based on the PApplet's active font.
     */
    private void setFont() {
        PFont pf = p.g.textFont;
        if (pf == null) {
            PGraphics.showException("Use textFont() before Book.addText()");
        }
        
        // try setting the Font from the PFont
        Font f = pf.getFont();
        if (f == null) {
            // try setting the Font directly
            PGraphics.showWarning("You should load your font using createFont() instead of loadFont() so that your sketch works on machines that don't have the font installed!");
            f = pf.findFont();
        }
        
        if (f == null) {
            PGraphics.showException("Cannot find the native version of the active PFont. Make sure it is installed on this machine!");
        }
        
        toBuilder.setTextAlign(p.g.textAlign);
        toBuilder.setFont(pf);
    }
    
    ///////////////////////////////////////////////////////////////////////////
	// Text building methods 
    
    /**
     * Builds a tree of TextObjects from the given string, at the specified 
     * location, using the stroke and fill colors set in the PApplet.
     * 
     * @param text the String to create the TextObjectGroup from
     * @param x the x-coordinate of the created TextObjectGroup
     * @param y the y-coordinate of the created TextObjectGroup
     * 
     * @return TextObjectGroup the built TextObjectGroup
     */
    public TextObjectGroup addText(String text, int x, int y) {
        setFont();
    	TextObjectGroup newTog = toBuilder.build(text, x, y);
    	setStrokeAndFill(newTog); 

        return newTog;
    }
    
    /**
     * Builds a tree of TextObjects on the given Page from the given string, 
     * at the specified location, using the stroke and fill colors set in 
     * the PApplet.
     * 
     * @param text the String to create the TextObjectGroup from
     * @param x the x-coordinate of the created TextObjectGroup
     * @param y the y-coordinate of the created TextObjectGroup
     * @param pageName the name of the Page to build on
     * 
     * @return TextObjectGroup the built TextObjectGroup
     */
    public TextObjectGroup addText(String text, int x, int y, String pageName) {
    	TextObjectGroup tempTog = toBuilder.getParent();
    	toBuilder.setParent(((TextPage)getPage(pageName)).getTextRoot());
    	TextObjectGroup newTog = addText(text, x, y);
    	toBuilder.setParent(tempTog);
    	return newTog;
    }
    
    /**
     * Builds a tree of TextObjects from the given string, at the specified
     * location, using the stroke and fill colors set in the PApplet.
     * 
     * @param text the String to create the TextObjectGroup from
     * @param x the x-coordinate of the created TextObjectGroup
     * @param y the y-coordinate of the created TextObjectGroup
     * @param lineLength the max number of characters per line
     * 
     * @return TextObjectGroup the built TextObjectGroup
     */
    public TextObjectGroup addText(String text, int x, int y, int lineLength) {
        setFont();
        TextObjectGroup newTog = toBuilder.buildSentence(text, x, y, lineLength);
    	setStrokeAndFill(newTog);

        return newTog;
    }
    
    /**
     * Builds a tree of TextObjects on the given Page from the given string, 
     * at the specified location, using the stroke and fill colors set in 
     * the PApplet.
     * 
     * @param text the String to create the TextObjectGroup from
     * @param x the x-coordinate of the created TextObjectGroup
     * @param y the y-coordinate of the created TextObjectGroup
     * @param lineLength the max number of characters per line
     * @param pageName the name of the Page to build on
     * 
     * @return TextObjectGroup the built TextObjectGroup
     */
    public TextObjectGroup addText(String text, int x, int y, int lineLength, String pageName) {
    	TextObjectGroup tempTog = toBuilder.getParent();
    	toBuilder.setParent(((TextPage)getPage(pageName)).getTextRoot());
    	TextObjectGroup newTog = addText(text, x, y, lineLength);
    	toBuilder.setParent(tempTog);
    	return newTog;
    }
    
    /**
     * Sets the stroke and fill properties based on the colors set in the PApplet.
     * 
     * @param to the TextObject to apply the color properties to
     */
    private void setStrokeAndFill(TextObject to) {
    	setStroke(to);
    	setFill(to);
    }
    
    /**
     * Sets the stroke properties based on the colors set in the PApplet.
     * 
     * @param to the TextObject to apply the color properties to
     */
    private void setStroke(TextObject to) {
    	ColorProperty colProp = to.getStrokeColor();
    	
    	if (p.g.stroke) {
    		// set the stroke cap
    		int cap;
	    	switch (p.g.strokeCap) {
	    	case PApplet.SQUARE:
	    		cap = BasicStroke.CAP_BUTT;
	    		break;
	    	case PApplet.PROJECT:
	    		cap = BasicStroke.CAP_SQUARE;
	    		break;
	    	default:
	    		cap = BasicStroke.CAP_ROUND;
	    		break;
	    	}
	    	
	    	// set the stroke join
	    	int join;
	    	switch (p.g.strokeJoin) {
	    	case PApplet.BEVEL:
	    		join = BasicStroke.JOIN_BEVEL;
	    		break;
	    	case PApplet.ROUND:
	    		join = BasicStroke.JOIN_ROUND;
	    		break;
	    	default:
	    		join = BasicStroke.JOIN_MITER;
	    		break;
	    	}
    	
	    	// set the stroke property
	    	StrokeProperty strokeProp = to.getStroke();
	    	strokeProp.setOriginal(new BasicStroke(p.g.strokeWeight, cap, join));
	    	strokeProp.set(new BasicStroke(p.g.strokeWeight, cap, join));
    		
	    	// set the stroke color property
	    	colProp.setOriginal(new Color(p.g.strokeColor, true));
	    	colProp.set(new Color(p.g.strokeColor, true));
            
    	} else {
    		// set the stroke color property to transparent
    		colProp.setOriginal(new Color(0, 0, 0, 0));
	    	colProp.set(new Color(0, 0, 0, 0));
        }
    }
    
    /**
     * Sets the fill properties based on the colors set in the PApplet.
     * 
     * @param to the TextObject to apply the color properties to
     */
    private void setFill(TextObject to) {
    	ColorProperty colProp = to.getColor();
    	
    	if (p.g.fill) {
            // set the fill color property
    		colProp.setOriginal(new Color(p.g.fillColor, true));
    		colProp.set(new Color(p.g.fillColor, true));
        } else {
        	// set the fill color property to transparent
        	colProp.setOriginal(new Color(0, 0, 0, 0));
        	colProp.set(new Color(0, 0, 0, 0));
        }
    }
    
    public void attachText(TextObject to) {
    	toBuilder.getParent().attachChild(to);
    }
    
    public void attachText(TextObject to, String pageName) {
    	TextObjectGroup tempTog = toBuilder.getParent();
    	toBuilder.setParent(((TextPage)getPage(pageName)).getTextRoot());
    	attachText(to);
    	toBuilder.setParent(tempTog);
    }
    
    ///////////////////////////////////////////////////////////////////////////
	// Behaviour management methods 
    
    /**
     * Adds the given Behaviour to the list of Behaviours applied to new TextObjectGlyphs.
     * <p>The Behaviour will only be added to TextObjects created after this method is called.</p>
     * <p>The Behaviour is automatically added to the NTPBook.</p>
     * 
     * @param b the Behaviour to add
     */
    public void addGlyphBehaviour(AbstractBehaviour b) { 
        toBuilder.addGlyphBehaviour(b);
        addBehaviour(b);
    }
    
    /**
     * Removes the given Behaviour from the list of Behaviours applied to new TextObjectGlyphs.
     * <p>The Behaviour will not be added to TextObjects created after this method is called.</p>
     * 
     * @param b the Behaviour to remove
     */
    public void removeGlyphBehaviour(AbstractBehaviour b) {
        toBuilder.removeGlyphBehaviour(b);
    }
    
    /**
     * Removes all Behaviours from the list of Behaviours applied to new TextObjectGlyphs.
     */
    public void removeAllGlyphBehaviours() {
        toBuilder.removeAllGlyphBehaviours();
    }
    
    /**
     * Adds the given Behaviour to the list of Behaviours applied to new TextObjectGroups.
     * <p>The Behaviour will only be added to TextObjects created after this method is called.</p>
     * <p>The Behaviour is automatically added to the NTPBook.</p>
     * 
     * @param b the Behaviour to add
     */
    public void addGroupBehaviour(AbstractBehaviour b) { 
        toBuilder.addGroupBehaviour(b);
        addBehaviour(b);
    }
    
    /**
     * Removes the given Behaviour from the list of Behaviours applied to new TextObjectGroups.
     * <p>The Behaviour will not be added to TextObjects created after this method is called.</p>
     * 
     * @param b the Behaviour to remove
     */
    public void removeGroupBehaviour(AbstractBehaviour b) {
        toBuilder.removeGroupBehaviour(b);
    }
    
    /**
     * Removes all Behaviours from the list of Behaviours applied to new TextObjectGroups.
     */
    public void removeAllGroupBehaviours() {
        toBuilder.removeAllGroupBehaviours();
    }
	
	///////////////////////////////////////////////////////////////////////////
	// Get methods
	
    /** Returns the page renderer */
	public TextPageRenderer getRenderer() { return defaultRenderer; }
    /** Returns the page set */
    public Collection<TextPage> getPages() { return pages.values(); }
	/** Returns the BehaviourList */
	public List<AbstractBehaviour> getBehaviourList() { return behaviourList; };
	/** 
     * Get the root of the TextObject hierarchy.  Any changes to this tree
     * must be synchronized on the book. Do not add textObjects as children of 
	 * the textRoot directly because they will not be rendered, add them to a TextPage instead.
     */
	public TextObjectRoot getTextRoot() { return textRoot; }
	/** Returns the Input Manager */
	public InputManager getInputs() { return inputs; }
	/** Returns the Spatial List */
	public SpatialList getSpatialList() { return spatialList; }
	
    /**
     * Add a page to the book without specifying a name.
     * 
     * <p>The page will be named "layerN" where N = 0,1,2,3...</p>
     * 
     */
    public void addPage(TextPage p){
        String name = "layer" + pages.size();
        pages.put(name,p);
    }

    /**
     * Add a named page to the book
     */
    public void addPage(String name, TextPage p){
        if (pages.containsKey(name)) {
        	log("WARNING: A Page with the name '"+name+"' already exists and will be deleted!");
        }
        pages.put(name,p);
    }
    
    /**
     * Create and add a named TextPage to the Book
     * 
     * @param pageName the name of the TextPage to add
     */
    public void addPage(String pageName) {
    	addPage(pageName, new TextPage(this, defaultRenderer));
    }
    
    /**
     * Get a named page from the book
     */
    public TextPage getPage(String name){
        return (TextPage)pages.get(name);
    }
	
	/**
	 * Adds a Behaviour to the Book.
	 */
	public void addBehaviour( AbstractBehaviour b ) {
	    behaviourList.add(b);
	}
	
	/**
	 * Removes a Behaviour from the Book.
	 */
	public void removeBehaviour( AbstractBehaviour b ) {
	    behaviourList.remove(b);
	}
    
    /**
     * Removes the children of this textObject from the book
     * @param to
     */
    public void removeChildren( TextObjectGroup to ){
        TextObject child = to.getLeftMostChild();
        while (child != null) {            
            removeObject(child);
            child = child.getRightSibling();
        }
    }
    
    /**
     * Removes all the TextObjects from all the TextPages in the Book, except for all the TextObjectRoots.
     */
    public void clear() {
    	Iterator<TextPage> i = pages.values().iterator();
        while (i.hasNext()) {
        	TextPage page = i.next();
            removeChildren(page.getTextRoot());
        }
    }
    
    /**
     * Removes all the TextObjects from the given TextPage, except for the TextObjectRoot.
     * @param pageName the name of the TextPage to clear
     */
    public void clearPage(String pageName) {
    	if (pages.get(pageName) instanceof TextPage) {
    		TextPage textPage = pages.get(pageName);
    		removeChildren(textPage.getTextRoot());
    	}
    }
    
    // font property setters and getters
    public void setLineHeight(double d) { toBuilder.setLineHeight(d); }
    public double getLineHeight() { return toBuilder.getLineHeight(); }
    public void setTrackingOffset(double d) { toBuilder.setTrackingOffset(d); }
    public double getTrackingOffset() { return toBuilder.getTrackingOffset(); }
    
    //////////////////////////////////////////////////////////////////////
    // Rudimentary Logging System

    PrintWriter logWriter = null;

    /**
     * Specify a PrintWriter to which all log messages will be written.
     *
     * <p>NextText generated messages are written to this PrintWriter.  </p>
     */
    public void setLogger(PrintWriter pw) {
        logWriter = pw;
    }

    /**
     * Log a message.
     *
     * <p>The message will go to standard output, as well as a log file if one
     * has been specified with setLogFile().  A newline will be appended to the
     * message.  </p>
     */
    public void log(String message) {
        System.out.println(message);
        if (logWriter != null) {
            synchronized (logWriter) {
                logWriter.println(message);
                logWriter.flush();
            }
        }
    }

    public void log(String message, Throwable t) {
        System.out.println(message);
        t.printStackTrace();
        if (logWriter != null) {
            synchronized (logWriter) {
                logWriter.println(message);
                t.printStackTrace(logWriter);
                logWriter.flush();
            }
        }
    }
}