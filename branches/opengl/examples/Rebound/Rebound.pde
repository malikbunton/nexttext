import processing.opengl.*;

import net.nexttext.*;
import net.nexttext.behaviour.control.*;
import net.nexttext.behaviour.physics.*;
import net.nexttext.behaviour.standard.*;

/**
 * A NextText physics example.
 * <p>Throw the letters around. For best results, release the mouse while it is in motion.</p>
 * 
 * <p>by Elie Zananiri | Obx Labs | February 2008<br>
 * Words by <a href="http://www.mitchhedberg.net/">Mitch Hedberg</a></p>
 */

// attributes
Book book;
PFont cheboygan;

void setup() {
  // init the applet
  size(640, 360, OPENGL);
  smooth();

  // create the book
  book = new Book(this);
  
  // create the actions
  Move move = new Move(0.01f, 0.01f);
  StayInWindow stayInWindow = new StayInWindow(this);

  MoveTo moveOverMouse = new MoveTo(Book.mouse, 100);
  Repeat followMouse = new Repeat(moveOverMouse, 0);
  MouseInertia mouseInertia = new MouseInertia(this, 0.5f, 0.01f);
  Throw throwAround = new Throw(followMouse, mouseInertia);
  moveOverMouse.setTarget(throwAround.getOnDrag());

  // add the behaviours to the book
  book.addGlyphBehaviour(move.makeBehaviour());
  book.addGlyphBehaviour(stayInWindow.makeBehaviour());
  book.addGlyphBehaviour(throwAround.makeBehaviour());
  
  // init and set the font
  cheboygan = createFont("Cheboygan.ttf", 48, true);
  textFont(cheboygan);
  textAlign(CENTER);
  
  // set the text colour
  fill(250, 5, 5, 200);
  noStroke();

  // add the text
  // doubling all the spaces for it to look better on screen
  book.addText("If  I  had  nine  of  my  fingers  missing  I  wouldn't  type  any  slower.", width/2, height/4, 30);
}

void draw() {
  background(0);

  // apply the behaviours to the text and draw it
  book.stepAndDraw();
}
