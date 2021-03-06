import java.util.ArrayList;
import java.util.Random;
import java.io.*;

// Attack vs expansion
// Within 8 of oppenent we stop expanding and only attack
//
// Tuning the heuristics
// Make need more than one deep
// Precomputing the best expansion path, by changing evalExpand
// Store oppenents name and the parameters you used, try different parameters that beat certain opponents
// 

public class MyExpJavaBot {
    public static final int RANGE = 7;
    public static final int MAX_STRENGTH = 255;
    public static final int THRESH = 40;
    public static final int P_THRESH = 2;
    public static Direction getOpposite(Direction d) {
        if (d == Direction.NORTH) {
            return Direction.SOUTH;
        } else if (d == Direction.SOUTH) {
            return Direction.NORTH;
        } else if (d == Direction.EAST) {
            return Direction.WEST;
        } else if (d == Direction.WEST) {
            return Direction.EAST;
        }
        return d;
    }
    public static Move makeMove(GameMap gameMap, int myID, Location loc) {
        Site s = gameMap.getSite(loc);
        if (s.next_strength + s.strength < MAX_STRENGTH + 100 && s.danger == 0) {
            s.next_strength += s.strength;
            return new Move(loc, Direction.STILL);
        }
        for (Direction d : Direction.CARDINALS) {
            Site n = gameMap.getSite(loc, d);
            if (n.next_strength + s.strength < MAX_STRENGTH + 100 && n.danger == 0) {
                n.next_strength += s.strength;
                return new Move(loc, d);
            }
        }
        s.next_strength += s.strength;
        return new Move(loc, Direction.STILL);

    }
    public static void set_enemy_distances(GameMap gameMap, int myID) {
        int dis = -1;
        //What is g- meaning? gdist gdir 
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                s.trydist = 1000;
                s.trymystr = 10000;
                s.tryresis = 10000; //More like accumulated resistance
                s.trydir = Direction.STILL;
                s.gdist = 30;
                s.gresis = 1000;
                s.gdir = Direction.STILL;
                s.bestg = 100;
                s.bestem = 0;
                s.moved = false;
                s.resis = 1000;
                s.dist = 10;
                s.dir = Direction.STILL;
                s.need = 0;
                s.next_strength = 0;
            }
        }
        while (dis < RANGE) {
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    Site s = gameMap.getSite(new Location(x, y));
                    //Initialize all the enemy squares as 0 trydist, tryresis, and trymystr
                    //On the first cycle
                    if (dis == -1 && s.owner != 0 && s.owner != myID) {
                        s.trydist = 0;
                        s.tryresis = 0;
                        s.trymystr = 0;
                    } else if (dis == s.trydist && dis == 0) { //This will be the second run through the squares
                        //Set squares around enemies and your squares to 1 dist, resistance to 0
                        for (Direction d : Direction.CARDINALS) {
                            Site n = gameMap.getSite(new Location(x, y), d);
                            if (n.owner == 0 || n.owner == myID) {
                                n.trydist = dis + 1;
                                n.tryresis = 0;
                                n.trymystr = s.trymystr;
                                n.bestem++; // What is best bestem
                            }
                        }
                    } else if (dis == s.trydist) {
                        for (Direction d : Direction.CARDINALS) {
                            Site n = gameMap.getSite(new Location(x, y), d);
                            int neighbourEval = eval_enemy(n.tryresis, n.trydist, n.trymystr) ;
                            int siteEval = eval_enemy(s.tryresis + s.strength, s.trydist + 1, s.trymystr);
                            int siteEvalPlusStr = eval_enemy(s.tryresis, s.trydist + 1, s.trymystr + s.strength);

                            if (s.owner == 0) {
                                //If a neutral square's neighbour's path is better, set
                                if (neighbourEval > siteEval) {
                                    n.trydist = dis + 1;
                                    n.tryresis = s.tryresis + s.strength;
                                    n.bestem = s.bestem;
                                    n.trymystr = s.trymystr;
                                    n.trydir = getOpposite(d);
                                } else if (neighbourEval == siteEval && n.bestem < s.bestem) {
                                    n.trydist = dis + 1;
                                    n.tryresis = s.tryresis + s.strength;
                                    n.bestem = s.bestem;
                                    n.trymystr = s.trymystr;
                                    n.trydir = getOpposite(d);
                                }
                            }
                            if (s.owner == myID) {
                                if (neighbourEval > siteEvalPlusStr) {
                                    n.trydist = dis + 1;
                                    n.tryresis = s.tryresis;
                                    n.bestem = s.bestem;
                                    n.trymystr = s.trymystr + s.strength;
                                    n.trydir = getOpposite(d);
                                } else if (neighbourEval == siteEvalPlusStr && n.bestem < s.bestem) {
                                    n.trydist = dis + 1;
                                    n.tryresis = s.tryresis;
                                    n.trymystr = s.trymystr + s.strength;
                                    n.bestem = s.bestem;
                                    n.trydir = getOpposite(d);
                                }
                            }
                        }
                    }
                }
            }
            dis++;
        }
        dis = -1;
        while (dis < 25) {
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    Site s = gameMap.getSite(new Location(x, y));
                    if (dis == -1 && s.owner == 0) {
                        s.gdist = 0;
                        s.gresis = 0;
                        s.bestg = 15 - s.production;
                    } else if (dis == s.gdist) {
                        for (Direction d : Direction.CARDINALS) {
                            Site n = gameMap.getSite(new Location(x, y), d);
                            if (s.owner == 0) {
                                if (eval_expan(gameMap, n.bestg, n.gdist, n.gresis) > eval_expan(gameMap, s.bestg, s.gdist+1, s.gresis + s.strength)) {
                                    n.gdist = dis + 1;
                                    n.gresis = s.gresis + s.strength;
                                    n.bestg = (s.bestg);
                                    n.gdir = getOpposite(d);
                                }
                            }
                            if (s.owner == myID) {
                                if (eval_expan(gameMap, n.bestg, n.gdist, n.gresis) > eval_expan(gameMap, s.bestg, s.gdist+1, s.gresis)) {
                                    n.gdist = dis + 1;
                                    n.gresis = s.gresis;
                                    n.bestg = s.bestg;
                                    n.gdir = getOpposite(d);
                                }
                            }
                        }
                    }
                }
            }
            dis++;
        }
    }
    public static int eval_enemy(int resis, int dist, int mystr) {
        return resis + (int)Math.pow((dist), 2)*2 + mystr/2;
    }
    public static int eval_expan(GameMap gameMap, int prod, int dist, int resis) {
        return prod*prod + (int) Math.pow(dist, 3) + 2*resis;
    }
    public static void notify_neighbors(GameMap gameMap, Location loc) {
        for (Direction d : Direction.CARDINALS) {
            Site n = gameMap.getSite(loc, d);
            n.danger = 1;
        }
    }
    public static void printfileRD(GameMap gameMap, int myID, String fname) throws Exception{
        PrintWriter pw = new PrintWriter(new FileWriter(fname));
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.owner == myID) {
                    pw.print("My " + s.gresis);
                } else {
                    pw.print(s.strength);
                }
                pw.print(" ");

            }
            pw.println();
        }
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.owner == myID) {
                    pw.print(s.bestg);
                } else {
                    pw.print(15 - s.production);
                }
                pw.print(" ");

            }
            pw.println();
        }
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.owner == myID) {
                    pw.print(s.gdist);
                } else if (s.owner == 0) {
                    pw.print(s.owner);
                } else {
                    pw.print(-1);
                }
                pw.print(" ");
            }
            pw.println();
        }
        pw.close();
    }
    public static void printfile(GameMap gameMap, int myID, String fname) throws Exception{
        PrintWriter pw = new PrintWriter(new FileWriter(fname));
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.owner == myID) {
                    if (s.gdir == Direction.STILL) {
                        pw.print("_");
                    } else if (s.gdir == Direction.NORTH) {
                        pw.print("N");
                    } else if (s.gdir == Direction.SOUTH) {
                        pw.print("S");
                    } else if (s.gdir == Direction.EAST) {
                        pw.print("E");
                    } else {
                        pw.print("W");
                    }
                } else {
                    pw.print(s.owner);
                }
                pw.print(" ");

            }
            pw.println();
        }
        pw.close();
    }
    public static void first_wave(GameMap gameMap, int myID, ArrayList<Move> moves) {
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                Site n = gameMap.getSite(new Location(x, y), s.trydir);
                if (s.owner == myID && s.trydist < 3) {
                    if (s.strength < THRESH || s.strength + n.next_strength > MAX_STRENGTH + 100) {
                        moves.add(makeMove(gameMap, myID, new Location(x, y)));
                    } else {
                        moves.add(new Move(new Location(x, y), s.trydir));
                        n.next_strength += s.strength;
                    }
                    s.moved = true;
                    s.need = 0;

                    if (s.trydist < 4 && s.strength >= THRESH) {
                        // System.out.println("lol");
                        notify_neighbors(gameMap, gameMap.getLocation(new Location(x, y), s.trydir));
                    }
                }
            }
        }
    }

    public static void attack(GameMap gameMap, int myID, ArrayList<Move> moves, int range) {
        first_wave(gameMap, myID, moves);
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.trydist < range && THRESH < s.strength && s.owner == myID && !s.moved) {
                    Site n = gameMap.getSite(new Location(x, y), s.trydir);
                    if (((n.strength < s.strength && n.owner == 0) || n.owner != 0) && n.danger == 0 && (n.next_strength + s.strength < MAX_STRENGTH + 50)) {
                        moves.add(new Move(new Location(x, y), s.trydir));
                        s.moved = true;
                        s.need = 0;
                        n.next_strength += s.strength;
                    } else {
                        moves.add(makeMove(gameMap, myID, new Location(x, y)));
                        s.moved = true;
                        s.need = n.strength - s.strength - s.production;
                        s.tstr = n.strength;
                        // s.next_strength += s.strength + s.production;
                    }
                }
            }
        }
    }
    // public static void check_better_neighbors(GameMap gameMap, int myID, Location loc) {
    //     Site mysite = gameMap.getSite(loc);
    //     for (Direction d : Direction.CARDINALS) {
    //         Site n = gameMap.getSite(loc, d);
    //         if (eval_enemy(n.resis, 1+n.dist) < , n.trymystreval_enemy(mysite.resis, mysite.dist) && d != mysite.dir && n.owner == myID) {
    //             mysite.dist = n.dist + 1;
    //             mysite.resis = n.resis;
    //             mysite.dir = d;
    //         }
    //     }
    // }
    public static void play_all_attacking_moves(GameMap gameMap, ArrayList<Move> moves, int myID, int turn) {
        // boolean done = false;
        // int i = 10;
        // while (!done) {
        //     for(int y = 0; y < gameMap.height; y++) {
        //         for(int x = 0; x < gameMap.width; x++) {
        //             //check if a neighbor has a better enemy_eval
        //             Site s = gameMap.getSite(new Location(x, y));
        //             if (!(s.moved || THRESH > s.strength) && s.owner == myID) {
        //                 if (s.dist == i) {
        //                     check_better_neighbors(gameMap, myID, new Location(x, y));
        //                 }
        //             }
        //         }
        //     }
        //     i--;
        //     if (i == 0) {
        //         done = true;
        //     }
        // }
        int val = 7;
        attack(gameMap, myID, moves, val);
        // try {
        //     printfile(gameMap, myID, "afAtt" + Integer.toString(turn));
        //     printfileRD(gameMap, myID, "afAttRD" + Integer.toString(turn));
        // } catch (Exception e) {

        // }
    }


    // public static void find_enemies(GameMap gameMap, Location loc, int myID) {
    //     Site mysite = gameMap.getSite(loc);

    //     //default values
    //     for (Direction d : Direction.CARDINALS) {
    //         int resistances = 1; //sum of resistances until enemy
    //         for (int i = 1; i < RANGE+1; i++) {
    //             Site unoccupied = gameMap.getSite(loc, d, i);
    //             if (unoccupied.owner != myID && unoccupied.owner != 0) { //if it's an enemy
    //                 if (eval_enemy(resistances, i) < eval_enemy(mysite.resis, mysite.dist)) {
    //                     mysite.resis = resistances;
    //                     mysite.dist = i;
    //                     mysite.dir = d;
                     
    //                 }
    //                 break;
    //             } else if (unoccupied.owner == 0) { //if it's neutral
    //                 resistances += unoccupied.strength;
    //             }
    //         }
    //     }
    // }

    public static void fill_enemies(GameMap gameMap, int myID, int turn) {
        for(int y = 0; y < gameMap.height; y++) {
            for(int x = 0; x < gameMap.width; x++) {
                Location loc = new Location(x, y);
                Site s = gameMap.getSite(loc);
                s.danger = 0;
                if (s.owner == myID) {
                    // find_enemies(gameMap, loc, myID);
                }
            }
        }
        // try {
        //     printfile(gameMap, myID, "befATT" + Integer.toString(turn));
        //     printfileRD(gameMap, myID, "befAttRD" + Integer.toString(turn));
        // } catch (Exception e) {

        // }
    }

    public static void makeTargetMove(GameMap gameMap, int myID, ArrayList<Move> moves) {
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site s = gameMap.getSite(new Location(x, y));
                if (s.gdir != Direction.STILL && s.owner == myID && !s.moved) {
                    Site n = gameMap.getSite(new Location(x, y), s.gdir);
                    if ((n.strength < s.strength && n.owner == 0) || (n.owner != 0 && s.strength > THRESH && s.strength + n.next_strength < 350)) {
                        moves.add(new Move(new Location(x, y), s.gdir));
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
                                moves.add(makeMove(gameMap, myID, gameMap.getLocation(new Location(x, y), d)));
                                n.moved = true;
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
                if (s.owner == myID && !s.moved && s.production*4 > s.strength) {
                    moves.add(makeMove(gameMap, myID, new Location(x, y)));
                    s.moved = true;
                    
                }
            }
        }
    }

    public static void get_targets(GameMap gameMap, Location loc, int myID) {
        Site mysite = gameMap.getSite(loc);
        mysite.tstr = 100000;
        mysite.tprod = -1;
        int bprod = 0;
        mysite.tdir = Direction.STILL;
        boolean winnable = false;
        Direction d = mysite.gdir;
        Site n = gameMap.getSite(loc, d);
        if (n.owner != myID) {
            if (mysite.strength > n.strength) {
                winnable = true;
                mysite.tstr = n.strength;
                mysite.tdir = d;
                mysite.tprod = n.production;
                bprod = n.production;
            } else {
                mysite.tstr = n.strength;
                mysite.tdir = d;
                mysite.tprod = n.production;
                bprod = n.production;
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
        
        if (mysite.gdir != Direction.STILL) {
            Site p = gameMap.getSite(loc, mysite.gdir);
            if (p.owner == myID && p.next_strength + mysite.strength < MAX_STRENGTH + 100) {
                moves.add(new Move(loc, mysite.gdir));
                p.next_strength += mysite.strength;
                mysite.moved = true;
                mysite.need = 0;
            }
        }
        if (!mysite.moved) {
            if (mysite.strength < 255) {
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
        int turn = 0;
        InitPackage iPackage = Networking.getInit();
        int myID = iPackage.myID;
        GameMap gameMap = iPackage.map;

        Networking.sendInit("ExpJavaBot");

        Random rand = new Random();

        while(true) {
            turn++;
            ArrayList<Move> moves = new ArrayList<Move>();
            gameMap = Networking.getFrame(); 

            //Find distances from your current block to the nearest enemy block
            set_enemy_distances(gameMap, myID);
            //For every block I own, find resistance and distance to nearest enemy
            //fill_enemies(gameMap, myID, turn);

            //Attacking moves are prioritized first, before expansion moves
            play_all_attacking_moves(gameMap, moves, myID, turn);

            //Expansion 
            find_targets(gameMap, myID);

            makeTargetMove(gameMap, myID, moves);

            makeNeedMove(gameMap, myID, moves);

            makeStillMove(gameMap, myID, moves);

            //Leftover Cells ??? Which pieces are we actually moving, what are the cases for this piece
            getLeftTargets(gameMap, myID);

            makeLeftMove(gameMap, moves, myID);

            Networking.sendFrame(moves);
        }
    }
}
