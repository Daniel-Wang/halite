import java.util.ArrayList;
import java.util.Random;

public class MyBot {
    public static void main(String[] args) throws java.io.IOException {
        InitPackage iPackage = Networking.getInit();
        int myID = iPackage.myID;
        GameMap gameMap = iPackage.map;

        Networking.sendInit("JavaBot");

        Random rand = new Random();

        while(true) {
            ArrayList<Move> moves = new ArrayList<Move>();

            gameMap = Networking.getFrame();

            for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                    Site site = gameMap.getSite(new Location(x, y));
                    if(site.owner == myID) {

                        boolean movedPiece = false;

                        int weakestNear = 400;
                        int highestProd = -1;
                        Direction weakestNearDir = Direction.STILL;

                        //Finds the weakest strength unoccupied square
                        for(Direction d : Direction.CARDINALS) {
                            Site unoccupied = gameMap.getSite(new Location(x, y), d);
                            //Find the weakest square
                            if (unoccupied.owner != myID && unoccupied.strength < weakestNear){
                                weakestNearDir = d;
                                weakestNear = unoccupied.strength;
                                highestProd = unoccupied.production;
                            } else if (unoccupied.owner != myID && unoccupied.strength == weakestNear){
                                //Find highest production square
                                if (unoccupied.production > highestProd){
                                    highestProd = unoccupied.production;
                                    weakestNearDir = d;
                                } 
                            }
                        }   

                        if(weakestNear != 400) {
                            if (site.strength < weakestNear){
                                moves.add(new Move(new Location(x, y), Direction.STILL));
                                movedPiece = true;
                            } else {
                                for(Direction d : Direction.CARDINALS) {
                                    Site unoccupied = gameMap.getSite(new Location(x, y), d);

                                    if(unoccupied.owner != myID && (unoccupied.strength == weakestNear) && (site.strength > unoccupied.strength)){ 
                                        moves.add(new Move(new Location(x, y), d));
                                        movedPiece = true;
                                        break;                             
                                    }
                                }
                            }                          
                        }


                        if(!movedPiece && weakestNear == 400){
                            for(int i = 2; i < (gameMap.width /2); i++){
                                for(Direction d : Direction.CARDINALS) {
                                    Site unoccupied = gameMap.getSite(new Location(x, y), d, i);
                                    if(unoccupied.owner != myID && (gameMap.getSite(new Location(x, y)).strength > gameMap.getSite(new Location(x, y)).production * 5)){
                                        moves.add(new Move(new Location(x, y), d));
                                        movedPiece = true;
                                        break; 
                                    } else if (unoccupied.owner != myID){
                                        moves.add(new Move(new Location(x, y), Direction.STILL));
                                        movedPiece = true;
                                        break;
                                    }
                                }                                
                            }
                        }

                        // if(!movedPiece) {
                        //     moves.add(new Move(new Location(x, y), rand.nextBoolean() ? Direction.NORTH : Direction.WEST));
                        //     movedPiece = true;
                        // }
                    }
                }
            }
            Networking.sendFrame(moves);
        }
    }

}