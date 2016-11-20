import java.util.ArrayList;
import java.util.Random;
import java.io.*;

public class MyBot {
    public static final int RANGE = 8;
    public static final int MAX_STRENGTH = 255;
    public static final int THRESH = 40;
    public static final int P_THRESH = 2;
    public static int eval_enemy(int resis, int dist) {
        return resis*(int)Math.pow(dist, 4);
    }
    public static void notify_neighbors(GameMap gameMap, Location loc) {
        for (Direction d : Direction.CARDINALS) {
            Site n = gameMap.getSite(loc, d);
            n.danger = 1;
        }
    }

    public static void first_wave(GameMap gameMap, int myID, ArrayList<Move> moves) {
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.owner == myID && s.dist == 3) {
                    moves.add(new Move(new Location(x, y), s.dir));
                    s.moved = true;
                    s.need = 0;
                    if (s.dist == 3) {
                        // System.out.println("lol");
                        notify_neighbors(gameMap, gameMap.getLocation(new Location(x, y), s.dir));
                    }
                }
            }
        }
    }

    public static void attack(GameMap gameMap, int myID, ArrayList<Move> moves) {
        first_wave(gameMap, myID, moves);
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.dist < RANGE && THRESH < s.strength && s.owner == myID && !s.moved) {
                    Site n = gameMap.getSite(new Location(x, y), s.dir);
                    if (((n.strength <= s.strength && n.owner == 0) || n.owner != 0) && n.danger == 0 && (n.next_strength + s.strength < MAX_STRENGTH + 150)) {
                        moves.add(new Move(new Location(x, y), s.dir));
                        s.moved = true;
                        s.need = 0;
                    } else {
                        moves.add(new Move(new Location(x, y), Direction.STILL));
                        s.moved = true;
                        s.need = n.strength - s.strength - s.production;
                        s.next_strength += s.strength + s.production;
                    }
                }
            }
        }
    }
    public static void check_better_neighbors(GameMap gameMap, int myID, Location loc) {
        Site mysite = gameMap.getSite(loc);
        for (Direction d : Direction.CARDINALS) {
            Site n = gameMap.getSite(loc, d);
            if (eval_enemy(n.resis, 1+n.dist) < eval_enemy(mysite.resis, mysite.dist) && d != mysite.dir && n.owner == myID) {
                mysite.dist = n.dist + 1;
                mysite.resis = n.resis;
                mysite.dir = d;
            }
        }
    }
    public static void play_all_attacking_moves(GameMap gameMap, ArrayList<Move> moves, int myID) {
        boolean done = false;
        int i = 10;
        while (!done) {
            for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                    //check if a neighbor has a better enemy_eval
                    Site s = gameMap.getSite(new Location(x, y));
                    if (!(s.moved || THRESH > s.strength) && s.owner == myID) {
                        if (s.dist == i) {
                            check_better_neighbors(gameMap, myID, new Location(x, y));
                        }
                    }
                }
            }
            i--;
            if (i == 0) {
                done = true;
            }
        }
        attack(gameMap, myID, moves);
    }

    public static void find_enemies(GameMap gameMap, Location loc, int myID) {
        Site mysite = gameMap.getSite(loc);

        //default values
        mysite.moved = false;
        mysite.resis = 100000;
        mysite.dist = 10;
        mysite.dir = Direction.STILL;
        mysite.need = 0;
        mysite.next_strength = 0;

        for (Direction d : Direction.CARDINALS) {
            int resistances = 1; //sum of resistances until enemy
            for (int i = 1; i < RANGE+1; i++) {
                Site unoccupied = gameMap.getSite(loc, d, i);
                if (unoccupied.owner != myID && unoccupied.owner != 0) { //if it's an enemy
                    if (eval_enemy(resistances, i) < eval_enemy(mysite.resis, mysite.dist)) {
                        mysite.resis = resistances;
                        mysite.dist = i;
                        mysite.dir = d;
                        break;
                    }
                } else if (unoccupied.owner == 0) { //if it's neutral
                    resistances += unoccupied.strength;
                }
            }
        }
    }

    public static void fill_enemies(GameMap gameMap, int myID) {
        for(int y = 0; y < gameMap.height; y++) {
            for(int x = 0; x < gameMap.width; x++) {
                Location loc = new Location(x, y);
                Site s = gameMap.getSite(loc);
                s.danger = 0;
                if (s.owner == myID) {
                    find_enemies(gameMap, loc, myID);
                }
            }
        }
    }

    public static void makeTargetMove(GameMap gameMap, int myID, ArrayList<Move> moves) {
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.tdir != Direction.STILL && s.owner == myID && !s.moved) {
                    Site n = gameMap.getSite(new Location(x, y), s.tdir);
                    if ((n.strength <= s.strength && n.owner == 0) || n.owner != 0) {
                        moves.add(new Move(new Location(x, y), s.tdir));
                        n.next_strength += s.strength;
                        s.moved = true;
                        s.need = 0;
                    }
                }
            }
        }        
    }

    public static void makeNeedMove(GameMap gameMap, int myID, ArrayList<Move> moves) {
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.owner == myID && !s.moved) {
                    for (Direction d : Direction.CARDINALS) {
                        Site n = gameMap.getSite(new Location(x, y), d);
                        if (n.owner == myID && n.need > 0 && n.need <= s.strength) {
                            moves.add(new Move(new Location(x, y), d));
                            n.need = 0;
                            n.next_strength += s.strength;
                            s.moved = true;
                            s.need = 0;
                            if (n.next_strength < n.tstr && !n.moved) {
                                moves.add(new Move(gameMap.getLocation(new Location(x, y), d), Direction.STILL));
                                n.moved = true;
                                n.next_strength += n.strength;
                            }
                        }
                    }
                }
            }
        }        
    }

    public static void makeStillMove(GameMap gameMap, int myID, ArrayList<Move> moves) {
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.owner == myID && !s.moved && THRESH > s.strength) {
                    moves.add(new Move(new Location(x, y), Direction.STILL));
                    s.moved = true;
                    s.next_strength += s.strength + s.production;
                }
            }
        }
    }

    public static void get_targets(GameMap gameMap, Location loc, int myID) {
        Site mysite = gameMap.getSite(loc);
        mysite.tstr = 100000;
        int bprod = 0;
        mysite.tdir = Direction.STILL;
        boolean winnable = false;
        for (Direction d : Direction.CARDINALS) {
            Site n = gameMap.getSite(loc, d);
            if (n.owner != myID) {
                if (mysite.strength >= n.strength) {
                    winnable = true;
                }
                if (n.strength < mysite.tstr - P_THRESH) {
                    mysite.tstr = n.strength;
                    mysite.tdir = d;
                    bprod = n.production;
                } else if (n.strength - P_THRESH <= mysite.tstr && n.production > bprod) {
                    mysite.tstr = n.strength;
                    mysite.tdir = d;
                    bprod = n.production;
                }
            }
        }
        if (!winnable && mysite.tdir != Direction.STILL) {
            mysite.need = mysite.tstr - mysite.strength - mysite.production;
        }
    }

    public static void find_targets(GameMap gameMap, int myID) {
        for(int y = 0; y < gameMap.height; y++) {
            for(int x = 0; x < gameMap.width; x++) {
                Location loc = new Location(x, y);
                Site s = gameMap.getSite(loc);
                if (s.owner == myID && !s.moved) {
                    get_targets(gameMap, loc, myID);
                }
            }
        }
    }

    public static void makeMyMove(GameMap gameMap, Location loc, int myID, ArrayList<Move> moves) {
        Site mysite = gameMap.getSite(loc);
        
        if (mysite.cdir != Direction.STILL) {
            Site p = gameMap.getSite(loc, mysite.cdir);
            if (p.owner == myID && p.next_strength + mysite.strength < MAX_STRENGTH + 100) {
                moves.add(new Move(loc, mysite.cdir));
                p.next_strength += mysite.strength;
                mysite.moved = true;
                mysite.need = 0;
            }
        }
        if (!mysite.moved) {
            if (mysite.strength < 230) {
                moves.add(new Move(loc, Direction.STILL));
                mysite.next_strength += mysite.strength + mysite.production;
                mysite.moved = true;
            } else {
                for (Direction d : Direction.CARDINALS) {
                    Site n = gameMap.getSite(loc, d);
                    if (n.owner == myID && n.next_strength + mysite.strength < MAX_STRENGTH + 100) {
                        moves.add(new Move(loc, d));
                        n.next_strength += mysite.strength;
                        mysite.moved = true;
                        mysite.need = 0;                     
                    }
                }
            }
        }
    }

    public static void find_something(GameMap gameMap, Location loc, int myID) {
        Site mysite = gameMap.getSite(loc);
        mysite.cdir = Direction.STILL;
        for (int i = 1; i < gameMap.height/2; i++) {
            for (Direction d : Direction.CARDINALS) {
                Site n = gameMap.getSite(loc, d, i);
                if (n.owner != myID) {
                    mysite.cdir = d;
                    return;
                }
            }
        }
    }

    public static void getLeftTargets(GameMap gameMap, int myID) {
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.owner == myID && !s.moved) {
                    find_something(gameMap, new Location(x, y), myID);
                }
            }
        }        
    }

    public static void makeLeftMove(GameMap gameMap, ArrayList<Move> moves, int myID) {
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.owner == myID && !s.moved) {
                    makeMyMove(gameMap, new Location(x, y), myID, moves);
                }
            }
        }        
    }

    public static void main(String[] args) throws java.io.IOException {
        InitPackage iPackage = Networking.getInit();
        int myID = iPackage.myID;
        GameMap gameMap = iPackage.map;

        Networking.sendInit("JavaBot");

        Random rand = new Random();

        while(true) {
            ArrayList<Move> moves = new ArrayList<Move>();
            gameMap = Networking.getFrame(); 

            //For every block I own, find resistance and distance to nearest enemy
            fill_enemies(gameMap, myID);

            play_all_attacking_moves(gameMap, moves, myID);
            //Expansion 

            find_targets(gameMap, myID);

            makeTargetMove(gameMap, myID, moves);

            makeNeedMove(gameMap, myID, moves);

            makeStillMove(gameMap, myID, moves);

            //Leftover Cells
            getLeftTargets(gameMap, myID);

            makeLeftMove(gameMap, moves, myID);

            Networking.sendFrame(moves);
        }
    }
}
