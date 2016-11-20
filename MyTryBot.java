import java.util.ArrayList;
import java.util.Random;
public class MyTryBot {
    public static float score(float pp, int struct) {
        return - (float) struct;
    }
    public static void main(String[] args) throws java.io.IOException {
        final int MAX_STRENGTH = 255;
        InitPackage iPackage = Networking.getInit();
        int myID = iPackage.myID;
        GameMap gameMap = iPackage.map;

        Networking.sendInit("CurrentTryJavaBot");

        Random rand = new Random();
        //Find all the production potentials of every square on the map
        for(int y = 0; y < gameMap.height; y++){
            for(int x= 0; x < gameMap.width; x++) {
                Site site = gameMap.getSite(new Location(x, y));
                site.tp = (float) site.production;
            }
        }

        for (int p = 0; p < 3; p++) {

        for(int y = 0; y < gameMap.height; y++){
            for(int x= 0; x < gameMap.width; x++) {
                Site site = gameMap.getSite(new Location(x, y));
                site.prodpot = -100000;
                for(Direction d : Direction.CARDINALS){
                    Site unoccupied = gameMap.getSite(new Location(x, y), d);
                    if(unoccupied.owner != myID && unoccupied.tp > site.prodpot*0.97f){
                        site.prodpot = unoccupied.tp*0.97f; 
                    }
                }
            }
        }
        for(int y = 0; y < gameMap.height; y++){
            for(int x= 0; x < gameMap.width; x++) {
                Site site = gameMap.getSite(new Location(x, y));
                site.tp = site.prodpot;
            }
        }
    }
        while(true) {
            ArrayList<Move> moves = new ArrayList<Move>();

            gameMap = Networking.getFrame();

            //Needs added
            for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                    Site site = gameMap.getSite(new Location(x, y));
                    int weakestNear = MAX_STRENGTH + 1;
                    int highestProd = -1;
                    float bestScore = -1000000;
                    Direction weakestNearDir = Direction.STILL;
                    site.next_strength = 0;
                        //Finds the weakest strength unoccupied square
                    for(Direction d : Direction.CARDINALS) {
                        Site unoccupied = gameMap.getSite(new Location(x, y), d);
                            //Find the weakest square
                        float s = score(unoccupied.prodpot, unoccupied.strength);
                        if (unoccupied.owner != myID && s > bestScore){
                            bestScore = s;
                            weakestNearDir = d;
                            weakestNear = unoccupied.strength;
                            highestProd = unoccupied.production;
                        }
                    }
                    if (weakestNear < MAX_STRENGTH) {
                        site.need = Math.max(weakestNear-site.strength, 0);    
                    } else {
                        site.need = 0;
                    }
                    site.dist = 10;
                    site.resis = 3000;
                    laser:
                    for(Direction d : Direction.CARDINALS) {
                        int sum = 0;
                        for (int i = 1; i < 8; i++ ) {
                            Site unoccupied = gameMap.getSite(new Location(x, y), d, i);

                            if(unoccupied.owner != myID && unoccupied.owner != 0 && i*sum < site.resis*site.dist) {
                                site.dist = i;
                                site.resis = sum;
                                site.dir = d;
                                break;
                            } else if (unoccupied.owner != myID) {
                                sum += unoccupied.strength;
                            }
                        }
                    }
                }
            }
                        
            //Actual Moves
            for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                    Site site = gameMap.getSite(new Location(x, y));
                    if(site.owner == myID) {

                        boolean movedPiece = false;
                        float bestScore = -1000000;
                        int weakestNear = MAX_STRENGTH + 1;
                        int highestNeed = 0;
                        Direction hnD = Direction.STILL;
                        int highestProd = -1;
                        Direction weakestNearDir = Direction.STILL;

                        //Fighting is the number one priority if it happen
                        for (Direction d : Direction.CARDINALS) {
                            Site unoccupied = gameMap.getSite(new Location(x, y), d);
                            if (site.production * 5 < site.strength && d != site.dir && unoccupied.dist < 8 && unoccupied.dist * unoccupied.resis + 200 < site.dist * site.resis) {
                                site.dist = unoccupied.dist + 1;
                                site.resis = unoccupied.resis;
                                site.dir = d;
                                site.need = MAX_STRENGTH + 1;
                                moves.add(new Move(new Location(x, y), d));
                                Site changed = gameMap.getSite(new Location(x, y), d);
                                changed.next_strength += site.strength;
                                movedPiece = true;
                            }
                        }
                        if (site.dist < 9 && site.production * 5 < site.strength) {
                            site.need = MAX_STRENGTH + 1;
                            moves.add(new Move(new Location(x, y), site.dir));
                            Site changed = gameMap.getSite(new Location(x, y), site.dir);
                            changed.next_strength += site.strength;
                            movedPiece = true;                           
                        }
                        if (movedPiece) continue;
                        //Finds the weakest strength unoccupied square
                        for(Direction d : Direction.CARDINALS) {
                            Site unoccupied = gameMap.getSite(new Location(x, y), d);
                            //Enemy square
                            float s = score(unoccupied.prodpot, unoccupied.strength);
                            if (unoccupied.owner != myID && s > bestScore){
                                bestScore = s;
                                weakestNearDir = d;
                                weakestNear = unoccupied.strength;
                                highestProd = unoccupied.production;
                            } else if (unoccupied.owner == myID) {
                                if (unoccupied.need > highestNeed && site.strength - unoccupied.need + unoccupied.production >= 0) {
                                    highestNeed = unoccupied.need;
                                    hnD = d;
                                }
                            }
                        }   

                        //If something cardinally adjacent is not our piece
                        // EXPANSION + (FIGHTING) (to be worked on)
                        if(weakestNear != MAX_STRENGTH + 1 && !movedPiece) {
                            if (site.strength < weakestNear){
                                if (highestNeed > 0) {
                                    // Fulfills the need, so other pieces don't try to do the same thing
                                    // and bad things happen
                                    site.need = MAX_STRENGTH + 1;
                                    moves.add(new Move(new Location(x, y), hnD));

                                    Site changed = gameMap.getSite(new Location(x, y), hnD);
                                    changed.next_strength += site.strength;
                                } else {
                                    moves.add(new Move(new Location(x, y), Direction.STILL));
                                    site.next_strength += site.strength;
                                }
                                movedPiece = true;
                            } else {
                                // Otherwise just move to the weakest square thats not ours
                                moves.add(new Move(new Location(x, y), weakestNearDir));
                                movedPiece = true;
                                Site changed = gameMap.getSite(new Location(x, y), weakestNearDir);
                                changed.next_strength += site.strength;
                            }                          
                        }

                        // If we are cardinally adjacent to our own pieces.
                        // EXPANSION
                        if(!movedPiece && weakestNear == MAX_STRENGTH + 1){
                            if (highestNeed > 0 && !movedPiece) {
                                site.need = MAX_STRENGTH+1;
                                moves.add(new Move(new Location(x, y), hnD));
                                Site changed = gameMap.getSite(new Location(x, y), hnD);
                                changed.next_strength += site.strength;
                                break;
                            } else {
                                for(int i = 2; i < (gameMap.width /2); i++){
                                    for(Direction d : Direction.CARDINALS) {
                                        Site unoccupied = gameMap.getSite(new Location(x, y), d, i);

                                        if(unoccupied.owner != myID && 
                                            (gameMap.getSite(new Location(x, y)).strength > gameMap.getSite(new Location(x, y)).production * 5)
                                             && ((gameMap.getSite(new Location(x, y), d).strength + site.strength <= 100+MAX_STRENGTH 
                                                && (gameMap.getSite(new Location(x, y), d).next_strength + site.strength <= 1.5*MAX_STRENGTH))) &&  !movedPiece) {
                                            site.need = MAX_STRENGTH+1;
                                            
                                            moves.add(new Move(new Location(x, y), d));
                                            Site changed = gameMap.getSite(new Location(x, y), d);
                                            changed.next_strength += site.strength;
                                            movedPiece = true;
                                            break;
                                        } else if (unoccupied.owner != myID && !movedPiece){
                                            moves.add(new Move(new Location(x, y), Direction.STILL));
                                            movedPiece = true;
                                            site.next_strength += site.strength;
                                            break;
                                        }
                                    }                                
                                }
                            }
                        }
                    }
                }
            }
            Networking.sendFrame(moves);
        }
    }

}