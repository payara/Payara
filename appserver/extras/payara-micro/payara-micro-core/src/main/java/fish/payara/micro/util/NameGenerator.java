/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.micro.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
/**
 *
 * @author Andrew Pielage
 */
public class NameGenerator
{
    private final String[] adjectives = new String[]{"Adorable", "Adventurous", "Aggressive", "Alert", "Beautiful", 
            "Blue-eyed", "Bloody", "Blushing", "Bright", "Clear", "Cloudy", "Colorful", "Crowded", "Cute", "Dark", 
            "Drab", "Distinct", "Dull", "Elegant", "Excited", "Fancy", "Filthy", "Glamorous", "Gleaming", "Gorgeous", 
            "Graceful", "Grotesque", "Handsome", "Heavy", "Homely", "Light", "Magnificent", "Misty", "Motionless", "Muddy", 
            "Old-fashioned", "Plain", "Poised", "Precious", "Quaint", "Shiny", "Sparkling", "Spotless", "Stormy", 
            "Strange", "Ugly", "Unusual", "Wide-eyed ", "Annoying", "Bad", "Beautiful", "Brainy", "Breakable", "Busy",
            "Careful", "Cautious", "Clever", "Clumsy", "Concerned", "Crazy", "Curious", "Dead", "Different", "Difficult", 
            "Doubtful", "Easy", "Famous", "Fragile", "Frail", "Gifted", "Helpful", "Helpless", "Horrible", "Important", 
            "Impossible", "Innocent", "Inquisitive", "Modern", "Mushy", "Odd", "Open", "Outstanding", "Poor", "Powerful", 
            "Prickly", "Puzzled", "Rich", "Shy", "Sleepy", "Super", "Talented", "Tame", "Tender", "Tough", "Uninterested", 
            "Vast", "Wandering", "Wild", "Agreeable", "Amused", "Brave", "Calm", "Charming", "Cheerful", "Comfortable", 
            "Cooperative", "Courageous", "Delightful", "Determined", "Eager", "Elated", "Enchanting", "Encouraging", 
            "Energetic", "Enthusiastic", "Excited", "Exuberant", "Fair", "Faithful", "Fantastic", "Fine", "Friendly", 
            "Funny", "Gentle", "Glorious", "Good", "Happy", "Healthy", "Helpful", "Hilarious", "Jolly", "Joyous", "Kind", 
            "Lively", "Lovely", "Lucky", "Nice", "Obedient", "Perfect", "Pleasant", "Proud", "Relieved", "Silly", 
            "Smiling", "Splendid", "Successful", "Thankful", "Thoughtful", "Victorious", "Vivacious", "Witty", "Wonderful",
            "Zealous", "Zany", "Bamboozled", "Magnanimous", "Humongous", "Confused", "Maleficent", "Sarcastic"};
    private final String[] fishes = new String[]{"Payara", "Catfish", "Tetra", "Goldfish", "Anchovy", "Shark", "Anglerfish",
            "Angelfish", "Pike", "Pufferfish", "Archerfish", "Char", "Cod", "Tuna", "Haddock", "Plaice", "Danio", 
            "Barracuda", "Swordfish", "Carp", "Batfish", "Barracudina", "Barramundi", "Blackfish", "Bass", "Boxfish", 
            "Butterfish", "Chimaera", "Chub", "Cisco", "Clownfish", "Cavefish", "Cowfish", "Cutlassfish", "Daggertooth", 
            "Dace", "Dab", "Devario", "Dartfish", "Dory", "Goby", "Loach", "Knifefish", "Elver", "Flatfish", "Featherback",
            "Flyingfish", "Footballfish", "Fusilier", "Guppy", "Hake", "Hawkfish", "Ide", "Jackfish", "Koi", "Ladyfish", 
            "Lionfish", "Minnow", "Medusafish", "Monkfish", "Nibbler", "Noodlefish", "Opaleye", "Paddlefish", "Pollock", 
            "Quillback", "Rainbowfish", "Rockfish", "Sablefish", "Sardine", "Scorpionfish", "Trout", "Skate", "Sole", 
            "Mackeral", "Dogfish", "Squeaker", "Spookfish", "Stonefish", "Turbot", "Unicornfish", "Velvetfish", "Wahoo",
            "Whitebait", "Whalefish", "Ziege", "Pirahna"};
    
    public String generateName() {
        int adjectivesIndex = ThreadLocalRandom.current().nextInt(0, adjectives.length);
        int fishIndex = ThreadLocalRandom.current().nextInt(0, fishes.length);
        
        String name = adjectives[adjectivesIndex] + "-" + fishes[fishIndex];
        
        return name;
    }
    
    public String generateUniqueName(List<String> takenNames, String UUID) {
        String name = "";
        
        // Generate a Map of all available names
        Map<String, List<String>> names = new HashMap<>();
        for (String adjective : adjectives) {
            names.put(adjective, Arrays.asList(fishes));
        }
        
        // Find a name not in use
        for (String adjective : names.keySet()) {
            // If a name has been found, exit the loop
            if (name.equals("")) {
                for (String fish : names.get(adjective)) {
                    String potentialName = adjective + "-" + fish;
                    if (!takenNames.contains(potentialName)) {
                        name = potentialName;
                        break;
                    }
                }
            } else {
                break;
            }
        }
        
        // If a unique name was not found, just set it to the instance UUID
        if (name.equals("")) {
            name = UUID;
        }
        
        return name;
    }
}
