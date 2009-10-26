import net.nexttext.*;
import net.nexttext.behaviour.*;
import net.nexttext.behaviour.control.*;
import net.nexttext.behaviour.physics.*;
import net.nexttext.behaviour.standard.*;
import net.nexttext.behaviour.dform.*;

/**
 * A NextText sketch where different behaviours can be applied
 * to the text using the 1, 2, 3, 4, 5 keys.
 *
 * by Elie Zananiri | Obx Labs | October 2009
 */

// global attributes
Book book; // the main object managing everything
Selector selector;  // the main action affecting the text
PFont font;  // the font used to display the text
String word = "NextText";  // the displayed text

void setup() {
  size(800, 360);
  smooth();

  // create the Book
  book = new Book(this);

  // load and set the font and color attributes
  font = createFont("GeometricBlack.ttf", 96, true);
  textFont(font);
  textAlign(CENTER, CENTER);
  fill(255);
  stroke(241, 100, 34);
  strokeWeight(5);
 
  // create the Selector and add all the Actions to it
  // a Selector is a control action that manages multiple
  // actions and allows to activate any one at runtime,
  // in this case, when keyboard keys are pressed.
  selector = new Selector();

  // Chain allows to string together actions sequentially
  Chain colour = new Chain();
  colour.add(new Colorize(Color.BLACK, 5)); // The first chained action will fade to black
  colour.add(new Colorize(Color.WHITE, 5)); // The second chained action will fade to white
  // Wrapping the Chain action in a Repeat action will repeat it indefinitely
  selector.add("Colour", new Repeat(colour));

  selector.add("RandomMotion", new RandomMotion());
  
  // Multiplexer allows to string together actions in unison
  Multiplexer physics = new Multiplexer();
  physics.add(new Move(0.01f, 0.01f));
  physics.add(new Gravity(4));
  selector.add("Physics", physics);
  
  // The Book.mouse passed to ChaosPull keeps the up-to-date mouse position
  selector.add("ChaosPull", new ChaosPull(Book.mouse));
  
  selector.add("Throb", new Throb(2, 100));
  
  // convert the action into a behaviour  
  Behaviour selectorBehaviour = selector.makeBehaviour();
  // add the Selector Behaviour to the Book to affect future glyphs
  // that are added
  book.addGlyphBehaviour(selectorBehaviour);
  book.addGlyphBehaviour(new StayInWindow(this));
  
  // select the Colour behavior as default
  selector.select("Colour");
  
  // add the text to the book, which uses all previously set
  // properties (font, color, behaviours, etc.)
  book.addText(word, width/2, height/2); 
}

void draw() {
  // clear the background
  background(0);
  
  // step (to apply behaviours) and
  // draw the book content
  book.stepAndDraw();
}

// activate the different actions added to the Selector when
// keys 1 to 5 are pressed
void keyPressed() {
  switch (key) {
    case '1':
      selector.select("Colour");
      break;
    case '2':
      selector.select("RandomMotion");
      break;
    case '3':
      selector.select("Physics");
      break;
    case '4':
      selector.select("ChaosPull");
      break;
    case '5':
      selector.select("Throb");
      break;
  }
}
