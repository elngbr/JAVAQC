// 1. Hey computer, look inside the folder named "com", then "gluonhq", then "strange", 
// //then "cloudlink", then "providers". That's where this file lives!
package com.gluonhq.strange.cloudlink.providers;

// 2. Go grab the "Recipe" notebook (Program) from another folder so we can use it here to make a quantum puzzle.
import org.redfx.strange.Program;
// 3. Go grab the "Prize Box" (Result) from another folder so we have a place to put the answers when the game is over.
import org.redfx.strange.Result;
// 4. Go grab Java's "Label Maker" tool (Map). It helps us match names to numbers, like "Player 1 = 50 points".
import java.util.Map;

/**
 * 5. This is the master Rulebook. It says: "If you want to be a Quantum Cloud club member 
 * (like IBM or D-Wave), you MUST know how to do these 4 specific actions."
 */
public interface QuantumCloudProvider {

    /**
     * 6. ACTION 1: The Mailman Action.
     * This takes your puzzle recipe, mails it to the cloud, runs 
     * it a bunch of times, and brings back the prize box.
     */
    // 7. Hand over the recipe (program), tell the machine how many times 
    // //to repeat it (shots), and wait for the prize box (Result).
    Result submitProgram(Program program, int shots) throws Exception;

    /**
     * 8. ACTION 2: The Scorekeeper Action.
     * This counts exactly how many times the spinning coins landed on Heads or Tails.
     */
    // 9. Run the recipe (program) a bunch of times (shots), and make a list matching the coin 
    // //patterns to how many times they happened.
    Map<String, Integer> getMeasurementCounts(Program program, int shots) throws Exception;

    /**
     * 10. ACTION 3: The Name Badge Action.
     * Every factory needs to shout its own name so we don't mix them up.
     */
    // 11. Give us a piece of text (String) that says 
    // //the factory's real name, like "IBM" or "Pasqal".
    String getProviderName();//!

    /**
     * 12. ACTION 4: The Doorbell Action.
     * Before we send code, we need to check if the lab is open or if we are locked out.
     */
    // 13. Answer YES (true) or NO (false) if the secret password works 
    // //and the quantum fridge is plugged in right now.
    boolean isAvailable();
}