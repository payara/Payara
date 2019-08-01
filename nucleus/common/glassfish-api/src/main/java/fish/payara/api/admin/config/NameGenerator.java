/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package fish.payara.api.admin.config;

import java.util.concurrent.ThreadLocalRandom;

public final class NameGenerator {
    // 161 different possible adjectives to describe a fish
    public static final String[] adjectives = new String[]{"Adorable", "Adventurous", "Aggressive", "Alert", "Beautiful",
            "Blue-eyed", "Bloody", "Blushing", "Bright", "Clear", "Cloudy", "Colourful", "Crowded", "Cute", "Dark",
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
            "Zealous", "Zany", "Bamboozled", "Magnanimous", "Humongous", "Confused", "Maleficent", "Sarcastic",
            "Sardonic", "Bemused", "Incandescent", "Furious", "Eccentric", "Laconic"};
    // 87 different sorts of amazing fish
    public static final String[] fishes = new String[]{"Payara", "Catfish", "Tetra", "Goldfish", "Anchovy", "Shark", "Anglerfish",
            "Angelfish", "Pike", "Pufferfish", "Archerfish", "Char", "Cod", "Tuna", "Haddock", "Plaice", "Danio",
            "Barracuda", "Swordfish", "Carp", "Batfish", "Barracudina", "Barramundi", "Blackfish", "Bass", "Boxfish",
            "Butterfish", "Chimaera", "Chub", "Cisco", "Clownfish", "Cavefish", "Cowfish", "Cutlassfish", "Daggertooth",
            "Dace", "Dab", "Devario", "Dartfish", "Dory", "Goby", "Loach", "Knifefish", "Elver", "Flatfish", "Featherback",
            "Flyingfish", "Footballfish", "Fusilier", "Guppy", "Hake", "Hawkfish", "Ide", "Jackfish", "Koi", "Ladyfish",
            "Lionfish", "Minnow", "Medusafish", "Monkfish", "Nibbler", "Noodlefish", "Opaleye", "Paddlefish", "Pollock",
            "Quillback", "Rainbowfish", "Rockfish", "Sablefish", "Sardine", "Scorpionfish", "Trout", "Skate", "Sole",
            "Mackeral", "Dogfish", "Squeaker", "Spookfish", "Stonefish", "Turbot", "Unicornfish", "Velvetfish", "Wahoo",
            "Whitebait", "Whalefish", "Ziege", "Piranha"};

    /**
     * Generates a new random name of the form adjective + fish.
     * <p>
     * This is not guranteeed to be unique.
     * @return
     */
    public static String generateName() {
        int adjectivesIndex = ThreadLocalRandom.current().nextInt(0, adjectives.length);
        int fishIndex = ThreadLocalRandom.current().nextInt(0, fishes.length);

        return adjectives[adjectivesIndex] + "-" + fishes[fishIndex];
    }
}
