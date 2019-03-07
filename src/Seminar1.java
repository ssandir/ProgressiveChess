import Rules.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

class Piece{
    int x = 0,y = 0, color;  //board[y][x] = (x,y) CUZ BOARD IS TILTED
    int[][] directions = new int[][]{};
    boolean scalable = false;

    //iterating procedures
    int it, scale;

    boolean isMoveValid(int[][] board, int xp, int yp){ //what name says
        return (yp < 8 && yp >= 0 && xp >= 0 && xp < 8 && color*board[yp][xp]<=0);
    }

    private void nextValidMove(int[][] board){ //also what function name says
        if(scalable){
            scale++;
            if(!isMoveValid(board,x+scale*directions[it][1],y+scale*directions[it][0])) {
                //if out of board or field is a friendly piece
                scale = 1;
                it++;
            }
        }
        else it++;

        while(it<directions.length && !isMoveValid(board,x+directions[it][1],y+directions[it][0])) {
            //if out of board or field is a friendly piece
            it++;
        }
    }

    void initIter(int[][] board){ //initiate iterator
        it=0;
        scale=1;
        if(!isMoveValid(board,x+directions[it][1],y+directions[it][0])){
            nextValidMove(board);
        }
    }

    /*  iterate through valid moves TODO: implement stop move when eating
        returns (y,x) cuz board is oriented WRONG*/
    int[] nextMove(int[][] board){
        if(it<directions.length){
            int[] r=new int[]{y,x};

            r[0]+=scale*directions[it][0];
            r[1]+=scale*directions[it][1];

            nextValidMove(board);

            return r;
        }
        return null;
    }
}

class King extends Piece{
    King(int x_in, int y_in, int color_in){
        x=x_in;
        y=y_in;
        directions = new int[][]{{1, 1}, {-1, 1}, {-1, -1}, {1, -1}, {1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        scalable = false;
        color=color_in;
    }
}

class Pawn extends Piece{
    Pawn(int color_in){
        directions = new int[][]{{color_in*1, -1}, {color_in*1, 1}};
    }

    Pawn(int x_in, int y_in, int color_in){
        x=x_in;
        y=y_in;
        directions = new int[][]{{color_in*1, 0}, {color_in*1, -1}, {color_in*1, 1}};
        scalable = false;
        color=color_in;
    }

    boolean isMoveValid(int[][] board, int xp, int yp){ //what name says
        if(!(yp < 8 && yp >= 0 && xp >= 0 && xp < 8)) return false;
        if(yp+directions[1][0]<8&&yp+directions[1][0]>=0){
            if(xp+1<8 && board[yp+directions[1][0]][xp+1]==color*-6) return false;
            if(xp-1>=0 && board[yp+directions[1][0]][xp-1]==color*-6) return false;
        }
        if(xp-x==0) return color*board[y][x]==0;
        else return color*board[y][x]<0;
    }

    ArrayList<int[]> validMoves(int[][] board){
        ArrayList r = new ArrayList<>();
        for(int i=0;i<3;++i){
            if(isMoveValid(board,x+directions[i][1],y+directions[i][0])) r.add(directions[i]);
        }
        return r;
    }
}

class Bishop extends Piece{
    Bishop(){
        directions = new int[][]{{1,1},{1,-1},{-1,-1},{1,1}};
    }

    Bishop(int x_in, int y_in, int color_in){
        x=x_in;
        y=y_in;
        directions = new int[][]{{1,1},{1,-1},{-1,-1},{1,1}};
        scalable = false;
        color=color_in;
    }
}

class Knight extends Piece{
    Knight(){
        directions = new int[][]{{1,2},{1,-2},{-1,2},{-1,-2},{2,1},{-2,1},{2,-1},{-2,-1}};
    }

    Knight(int x_in, int y_in, int color_in){
        x=x_in;
        y=y_in;
        directions =  new int[][]{{1,2},{1,-2},{-1,2},{-1,-2},{2,1},{-2,1},{2,-1},{-2,-1}};
        scalable = false;
        color=color_in;
    }
}

public class Seminar1 {
    static int nodecount;

    private final static int h_CHECKMATE = 1000000;
    private final static int const_hm = 8;
    private final static int const_a = 100;
    private final static int const_base = 1000;
    private final static int const_cvrd = 25;
    private final static boolean use_dfs = false;
    private final static boolean use_ecvrd = false;
    private final static int const_ecvrd = 10;
    private final static int const_onemove = -50;
    private final static int const_omdist = -120;
    private final static int const_bd = 20; //bishop
    private final static int const_kd = 5; //knight
    private final static int const_rd = 10; //rook
    private final static int const_path = 20;
    private final static int const_promo = 60;
    private final static int const_cap = 30;
    private final static boolean use_fen = true;
    private final static boolean use_inhe = false;
    private static int foundFasterMate;

    private static int mhDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private static class Sequence implements Comparator<Sequence> {
        Chessboard board;
        public ArrayList<Move> seq;
        public int h;
        private boolean initial;

        //stats
        public static int color;
        public static King eKing;
        private static int fnc; //first not covered
        private static int[][] checkFF; //check fields with checking figure
        private static int[][] checkFB; //blockable check fields
        private static int[][] IECFK; //initial enemy covered fields with king
        private static int[][] IECF; //initial enemy covered fields without king

        private boolean calculated;
        private int[][] boardA;  // (y,x) WHO MADE THIS RETARDED TILTED BOARD
        private int nnc; //now not covered
        private int[][] ECFK;
        private int[][] ECF;


        //last move data
        public int from_x, from_y, to_x, to_y, piece, promo, cap;

        private int pieceToInt(char c) {
            switch (c) {
                case 'O':
                    return 6;
                case 'K':
                    return 6;
                case 'Q':
                    return 5;
                case 'R':
                    return 4;
                case 'N':
                    return 3;
                case 'B':
                    return 2;
                default:
                    return 1;
            }
        }

        private int fieldToInt(char c){
            switch(c) {
                case 'a':
                    return 0;
                case 'b':
                    return 1;
                case 'c':
                    return 2;
                case 'd':
                    return 3;
                case 'e':
                    return 4;
                case 'f':
                    return 5;
                case 'g':
                    return 6;
                default:
                    return 7;
            }
        }

        private void getLastMoveData(Move move){
            move.setAlgebraicNotaion(true);
            String alg = move.toString();
            move.setAlgebraicNotaion(false);
            String nalg = move.toString();
            piece = pieceToInt(alg.charAt(0));
            from_x = fieldToInt(nalg.charAt(0));
            from_y = ((int) nalg.charAt(1)) - 49;
            to_x = fieldToInt(nalg.charAt(3));
            to_y = ((int) nalg.charAt(4)) - 49;
            promo=-1;
            cap = alg.contains("x")?1:0;
            //System.out.println(nalg.charAt(nalg.length()-1));
            if(!Character.isDigit(nalg.charAt(nalg.length()-1))) promo = pieceToInt(nalg.charAt(nalg.length()-1));
        }
        //last move data

        //init
        Sequence(){}

        Sequence(@NotNull Chessboard board_in){ //initial empty sequence
            seq = new ArrayList<Move>();
            initial=true;
            h=const_base;
            board = board_in.copy();

            //stats init
            calculated=false;
            color = board.getColor();
            boardA = board.getBoard();
            loop1: for(int i=0;i<boardA.length;++i){
                for(int j=0;j<boardA[i].length;++j){
                    if(boardA[i][j] == color*board.KING_B){
                        eKing=new King(j,i,-color);
                        break loop1;
                    }
                }
            }
            fnc = getNnc();

            if(use_ecvrd) {
                checkFF = new int[8][8];
                checkFB = new int[8][8];
                IECF = getECF();
                IECFK = getECFK();
            }
        }

        Sequence(@NotNull Sequence sequence, Move move, Chessboard board_in, int h_in){ //extended sequence
            initial=false;
            seq = new ArrayList<Move>(sequence.seq);
            seq.add(move);
            h=h_in;
            board=board_in;
            calculated=false;
            getLastMoveData(move);
        }
        //init

        //calculate heuristics
        //harder to calculate values
        public void calculate(){
            calculated=true;
            boardA=board.getBoard();

            //not covered count
            eKing.initIter(boardA);
            nnc=0;
            int[] move = eKing.nextMove(boardA);
            while(move != null){
                if(!Rules.isCovered(boardA,move[0],move[1],color)) nnc++;
                move = eKing.nextMove(boardA);
            }

            //ECF/ECFK
            if(use_ecvrd) {
                ECF = new int[8][8];
                ECFK = new int[8][8];
                if (initial) {
                    for (int i = 0; i < 8; ++i) {
                        for (int j = 0; j < 8; ++j) {
                            ECFK[i][j] = Rules.isCovered(boardA, i, j, -color) ? 1 : 0;
                        }
                    }
                    boardA[eKing.y][eKing.x] = 0;
                    for (int i = 0; i < 8; ++i) {
                        for (int j = 0; j < 8; ++j) {
                            ECF[i][j] = Rules.isCovered(boardA, i, j, -color) ? 1 : 0;
                        }
                    }
                    boardA[eKing.y][eKing.x] = color * board.KING_B;
                } else {
                    for (int i = 0; i < 8; ++i) {
                        for (int j = 0; j < 8; ++j) {
                            if (checkFF[i][j] > 0) ECFK[i][j] = Rules.isCovered(boardA, i, j, -color) ? 1 : 0;
                        }
                    }
                    boardA[eKing.y][eKing.x] = 0;
                    for (int i = 0; i < 8; ++i) {
                        for (int j = 0; j < 8; ++j) {
                            if (checkFB[i][j] > 0) ECF[i][j] = Rules.isCovered(boardA, i, j, -color) ? 1 : 0;
                        }
                    }
                    boardA[eKing.y][eKing.x] = color * board.KING_B;
                }
            }
        }

        public int getNnc(){
            if(!calculated){
                calculate();
            }

            return nnc;
        }

        public int[][] getECF(){
            if(!calculated){
                calculate();
            }

            return ECF;
        }

        public int[][] getECFK(){
            if(!calculated){
                calculate();
            }

            return ECFK;
        }

        private int calcECFval(){
            // blockable fields  x without king
            // check figures x with king
            getECF();
            getECFK();
            int sumI = 0, sumN = 0;
            for(int i=0;i<8;++i){
                for(int j=0;j<8;++j){
                    sumI += checkFF[i][j]*IECFK[i][j] + checkFB[i][j]*IECF[i][j];
                    sumN += checkFF[i][j]*ECFK[i][j] + checkFB[i][j]*ECF[i][j];
                }
            }
            if(sumI>0) {
                return ((sumI - sumN) * const_ecvrd) / sumI;
            }
            return 0;
        }

        public int evalh() {

            int boardStatus = board.getGameStatus();
            if (boardStatus == board.CHECKMATE) { //checkmategetNnc()
                if (board.getMovesLeft() > 0) {   //wrong time for mate
                    h = -1;
                    foundFasterMate = 1;
                    //TODO: re-evaluate all after this
                } else h = h_CHECKMATE; //right time, found solution
            }
            else if (board.getMovesLeft() == 0) h = -1;  //out of moves, discard
            else if (boardStatus == board.CHECK){
                h = -1;
                if(use_ecvrd) findCheckFields();
            } //check, calc check fields then discard
            else if (boardStatus == board.DRAW) h = -1;//invalid move discard
            else {
                int change = const_base + const_a / (board.getMovesLeft() + 3); //higher priority for moves further in + base

                if(use_dfs){
                    h = change;
                    return h;
                }

                change += (fnc - getNnc()) * const_cvrd; //covered fields
                if(use_ecvrd) change += calcECFval(); //minimize check fields covered by opponent

                int cd = mhDistance(eKing.x,eKing.y,to_x,to_y);
                if(cd<4&&cd>1) change += const_cap*cap;

                //punish generally more useless moves
                if(foundFasterMate == 0) {
                    //king or pawn single move punishment
                    if (piece == 1){
                        if(from_y == (color == 1 ? 1 : 6) && Math.abs(from_y - to_y) == 1) change += const_onemove;
                        if(from_y == (color == 1 ? 6 : 1)) change += const_promo;
                        int dist = 10;
                        if(Math.abs(eKing.x-to_x)<3) dist = Math.abs(eKing.y-to_y) - (eKing.x-to_x == 0?2:1); //TODO: check this recursively with pawn moves on board
                        dist = Math.min(dist, Math.abs(to_x - (color==1?7:0)));
                        if(dist>=board.getMovesLeft()){
                            change +=const_omdist/(board.getMovesLeft() + 1);
                            if(from_y == (color == 1 ? 1 : 6) && Math.abs(from_y - to_y) == 1) change += const_onemove;
                        }
                    }

                    if (piece == 6) {
                        change += const_onemove; //constant for bad king move

                        int pdist = Math.max(Math.abs(eKing.x-from_x),Math.abs(eKing.y-from_y));
                        int dist = Math.max(Math.abs(eKing.x-to_x),Math.abs(eKing.y-to_y));
                        if(dist>board.getMovesLeft()+1) change +=const_omdist/(board.getMovesLeft()+2);
                    }
                    //king or pawn single move punishment
                }

                //revard generally good moves
                if(piece == board.BISHOP || piece == board.QUEEN || piece == board.KING){
                    int dist = Math.max(Math.abs(eKing.x-to_x),Math.abs(eKing.y-to_y));
                    int pdist = Math.max(Math.abs(eKing.x-from_x),Math.abs(eKing.y-from_y));
                    if(pdist>2){
                        dist=Math.max(dist,2);
                        change+=(pdist-dist)*const_bd;
                        if(piece == board.KING && pdist>dist && dist < board.getMovesLeft() + 2){
                            change+=const_onemove/3+5;
                        }
                    }
                }
                if(piece == board.KNIGHT){
                    int dist = mhDistance(eKing.x, eKing.y, to_x, to_y);
                    int pdist = mhDistance(eKing.x, eKing.y, from_x, from_y);
                    dist=Math.max(dist,2);
                    if(pdist>2){
                        change+=(pdist-dist)*const_kd;
                    }
                    if(dist>3&&(to_x==0||to_y==0||to_x==7||to_y==7)){
                        change+=const_onemove;
                    }
                }
                if(piece == board.ROOK || piece == board.QUEEN || piece == board.KING){
                    int rookBonus = 0;
                    int dist = mhDistance(eKing.x, eKing.y, to_x, to_y);
                    int pdist = mhDistance(eKing.x, eKing.y, from_x, from_y);
                    if(pdist>2){
                        rookBonus+=(pdist-dist)*const_rd;
                        if(piece == board.KING && pdist>dist && dist < board.getMovesLeft() + 2){
                            rookBonus+=(pdist-dist)*const_onemove/3+5;
                        }
                    }
                    if(Math.abs(dist-pdist)==1) rookBonus-=const_rd;

                    if(piece!= board.KING){} //TODO: second direction check

                    change+=rookBonus;
                }

                //free up bonus
                boardA = board.getBoard();
                if(isBlocking(from_x,from_y,2)) change += const_path * ((board.getMovesLeft()-1)/2);
                if(isBlocking(from_x,from_y,1)) change += const_path * ((board.getMovesLeft()+1)/3);

                //TODO: promotion bonus?
                //TODO: evaluate move based on fields near king, king move (done), pieces protect (done), pin
                //TODO: moves that free up  pieces higher rated
                h=use_inhe?(h+change)/2:change;
            }
            return h;
        }

        private void findCF(){
            int cpx = eKing.x;
            int cpy = eKing.y;
            while(cpx+1<8){
                cpx++;
                if(boardA[cpy][cpx]==color*board.ROOK || boardA[cpy][cpx]==color*board.QUEEN){
                    checkFF[cpy][cpx]++;
                    cpx--;
                    while(cpx!=eKing.x){
                        checkFB[cpy][cpx]++;
                        cpx--;
                    }
                    break;
                }
                else if(boardA[cpy][cpx]!=0) break;
            }

            cpx = eKing.x;
            cpy = eKing.y;
            while(cpx-1>=0){
                cpx--;
                if(boardA[cpy][cpx]==color*board.ROOK || boardA[cpy][cpx]==color*board.QUEEN){
                    checkFF[cpy][cpx]++;
                    cpx++;
                    while(cpx!=eKing.x){
                        checkFB[cpy][cpx]++;
                        cpx++;
                    }
                    break;
                }
                else if(boardA[cpy][cpx]!=0) break;
            }

            cpx = eKing.x;
            cpy = eKing.y;
            while(cpy+1<8){
                cpy++;
                if(boardA[cpy][cpx]==color*board.ROOK || boardA[cpy][cpx]==color*board.QUEEN){
                    checkFF[cpy][cpx]++;
                    cpy--;
                    while(cpx!=eKing.x){
                        checkFB[cpy][cpx]++;
                        cpy--;
                    }
                    break;
                }
                else if(boardA[cpy][cpx]!=0) break;
            }

            cpx = eKing.x;
            cpy = eKing.y;
            while(cpy-1>=0){
                cpy--;
                if(boardA[cpy][cpx]==color*board.ROOK || boardA[cpy][cpx]==color*board.QUEEN){
                    checkFF[cpy][cpx]++;
                    cpy++;
                    while(cpx!=eKing.x){
                        checkFB[cpy][cpx]++;
                        cpy++;
                    }
                    break;
                }
                else if(boardA[cpy][cpx]!=0) break;
            }

            Bishop b=new Bishop();
            for(int i=0;i<b.directions.length;++i){
                int dirx = b.directions[i][1];
                int diry = b.directions[i][0];
                cpx = eKing.x;
                cpy = eKing.y;
                while(cpx + dirx >= 0 && cpx + dirx < 8 && cpy + diry >= 0 && cpy + diry < 8){
                    cpx+=dirx;
                    cpy+=diry;
                    if(boardA[cpy][cpx]==color*board.BISHOP || boardA[cpy][cpx]==color*board.QUEEN){
                        checkFF[cpy][cpx]++;
                        cpx-=dirx;
                        cpy-=diry;
                        while(cpx!=eKing.x){
                            checkFB[cpy][cpx]++;
                            cpx-=dirx;
                            cpy-=diry;
                        }
                        break;
                    }
                    else if(boardA[cpy][cpx]!=0) break;
                }
            }

            Knight k=new Knight();
            for(int i=0;i<k.directions.length;++i){
                int dirx = k.directions[i][1];
                int diry = k.directions[i][0];
                cpx = eKing.x + dirx;
                cpy = eKing.y + diry;
                if(cpx >= 0 && cpx < 8 && cpy >= 0 && cpy < 8 && boardA[cpy][cpx]==color*board.KNIGHT){
                    checkFF[cpy][cpx]++;
                }
            }

            Pawn p = new Pawn(-color);
            for(int i=0;i<p.directions.length;++i){
                int dirx = p.directions[i][1];
                int diry = p.directions[i][0];
                cpx = eKing.x + dirx;
                cpy = eKing.y + diry;
                if(cpx >= 0 && cpx < 8 && cpy >= 0 && cpy < 8 && boardA[cpy][cpx]==color*board.PAWN){
                    checkFF[cpy][cpx]++;
                }
            }
        }

        private boolean isBlocking(int px, int py, int pc){
            int cpx = px;
            int cpy = py;
            int pcs=0;
            while(cpx+1<8 && pcs<pc  ){//&& eKing.x <= px){
                cpx++;
                if(boardA[cpy][cpx]==color*board.ROOK || boardA[cpy][cpx]==color*board.QUEEN) return true;
                else if(boardA[cpy][cpx]*color > 0) pcs++;
                else if(boardA[cpy][cpx] == 0) continue;
            }

            cpx = px;
            cpy = py;
            pcs=0;
            while(cpx-1>0 && pcs<pc ){//&& eKing.x >= px){
                cpx--;
                if(boardA[cpy][cpx]==color*board.ROOK || boardA[cpy][cpx]==color*board.QUEEN) return true;
                else if(boardA[cpy][cpx]*color > 0) pcs++;
                else if(boardA[cpy][cpx] == 0) continue;
            }

            cpx = px;
            cpy = py;
            pcs=0;
            while(cpy+1<8 && pcs<pc ){//&&eKing.y <= py){
                cpy++;
                if(boardA[cpy][cpx]==color*board.ROOK || boardA[cpy][cpx]==color*board.QUEEN) return true;
                else if(boardA[cpy][cpx]*color > 0) pcs++;
                else if(boardA[cpy][cpx] == 0) continue;
            }

            cpx = px;
            cpy = py;
            pcs=0;
            while(cpy-1>0 && pcs<pc ){//&& eKing.y >= py){
                cpy--;
                if(boardA[cpy][cpx]==color*board.ROOK || boardA[cpy][cpx]==color*board.QUEEN) return true;
                else if(boardA[cpy][cpx]*color > 0) pcs++;
                else if(boardA[cpy][cpx] == 0) continue;
            }


            /*Bishop b=new Bishop();
            for(int i=0;i<b.directions.length;++i){
                int dirx = b.directions[i][1];
                int diry = b.directions[i][0];
                cpx = px;
                cpy = py;
                pcs=0;
                while(cpx + dirx >= 0 && cpx + dirx < 8 && cpy + diry >= 0 && cpy + diry < 8 && pcs<pc
                        && Math.signum(eKing.x-px) != dirx && Math.signum(eKing.y-py) != diry){
                    cpx+=dirx;
                    cpy+=diry;
                    if(boardA[cpy][cpx]==color*board.BISHOP || boardA[cpy][cpx]==color*board.QUEEN){
                        return true;
                    }
                    else if(boardA[cpy][cpx]*color > 0) pcs++;
                    else if(boardA[cpy][cpx] == 0) continue;
                }
            }*/
            return false;
    /*
            Knight k=new Knight();
            for(int i=0;i<k.directions.length;++i){
                int dirx = k.directions[i][1];
                int diry = k.directions[i][0];
                cpx = eKing.x + dirx;
                cpy = eKing.y + diry;
                if(cpx >= 0 && cpx < 8 && cpy >= 0 && cpy < 8 && boardA[cpy][cpx]==color*board.KNIGHT){
                    checkFF[cpy][cpx]++;
                }
            }

            Pawn p = new Pawn(-color);
            for(int i=0;i<p.directions.length;++i){
                int dirx = p.directions[i][1];
                int diry = p.directions[i][0];
                cpx = eKing.x + dirx;
                cpy = eKing.y + diry;
                if(cpx >= 0 && cpx < 8 && cpy >= 0 && cpy < 8 && boardA[cpy][cpx]==color*board.PAWN){
                    checkFF[cpy][cpx]++;
                }
            }
            */
        }

        public void findCheckFields(){
            boardA=board.getBoard();
            findCF();
        }

        public void findCheckFields(Move move){
            if(use_ecvrd) {
                boardA = board.getBoard();
                getLastMoveData(move);
                int save = boardA[to_y][to_x];
                boardA[to_y][to_x] = boardA[from_y][from_x];
                boardA[from_y][from_x] = 0; //artificially make move

                findCF(); //calculate

                boardA[from_y][from_x] = boardA[to_y][to_x];
                boardA[to_y][to_x] = save; //reverse artificial move
            }
        }
        //calculate heuristics


        public int len(){
            return seq.size();
        }

        public int compare(Sequence s1, Sequence s2){
            return s2.h - s1.h;
        }

        public ArrayList<String> to_strings(int type){
            ArrayList<String> r = new ArrayList<>();
            for (Move move : seq) {
                if(type == 0){
                    move.setAlgebraicNotaion(false);
                    r.add(move.toString());
                }
                else if(type > 0){
                    move.setAlgebraicNotaion(true);
                    r.add(move.toString());
                }
                if (type == 2){
                    move.setAlgebraicNotaion(false);
                    r.set(r.size()-1, r.get(r.size()-1).charAt(0) + move.toString());
                }
            }
            return r;
        }

        public String to_string(){
            return String.join(";",to_strings(0));
        }

        public String toKey(){
            if(use_fen){
                boardA = board.getBoard();
                int save = boardA[to_y][to_x];
                boardA[to_y][to_x] = boardA[from_y][from_x];
                if(promo>0){
                    boardA[to_y][to_x] = color*promo;
                }
                boardA[from_y][from_x] = 0; //artificially make move

                String[] str = board.getFEN().split(" ");

                boardA[from_y][from_x] = boardA[to_y][to_x];
                if(promo>0) boardA[from_y][from_x] = color*1;
                boardA[to_y][to_x] = save; //reverse artificial move
                return str[0];
            }

            ArrayList<String> moves=to_strings(2);
            Collections.sort(moves);
            return String.join("", moves);
        }
    }

    @Nullable private static String Astar(Chessboard board){
        long stime = System.nanoTime();
        foundFasterMate = 0;
        ArrayList<Move> possibleMoves;
        Sequence ns;  //new sequence (temporary)
        int h;//temp eval variable
        String moveStr=" ";

        //initialize
        PriorityQueue<Sequence> queue = new PriorityQueue<>(80, new Sequence());
        queue.add(new Sequence(board));
        HashMap<String, Sequence> HM = new HashMap();

        int count = 0;
        int count2 =0;

        long measure=0;
        long measure2=0;
        long measure3=0;
        long measureTemp;
        long measureTemp2;


        while((System.nanoTime()-stime)/1000000 < 19800){
        //count<1000000000){
                    measureTemp=System.nanoTime();
            Sequence cs = queue.poll(); //current sequence
            board=cs.board;
            possibleMoves = board.getMoves();
                    measure+=System.nanoTime()-measureTemp;
                    measureTemp=System.nanoTime();
            for(Move move:possibleMoves) {
                count++;
                if(board.getMovesLeft()==1){
                            measureTemp2 = System.nanoTime();
                    move.setAlgebraicNotaion(true);
                    moveStr=move.toString();
                    if(moveStr.charAt(moveStr.length()-1)=='#'){
                                nodecount+=count;
                                System.out.print(count + " ");
                                System.out.print(count2 + " ");
                                System.out.print(measure/1000000+ " ");
                                System.out.print(measure2/1000000 + " ");
                                System.out.print(measure3/1000000 + " ");
                        return (new Sequence(cs, move, board, cs.h)).to_string();
                    }
                    else if(moveStr.charAt(moveStr.length()-1)=='+' && use_ecvrd){
                        cs.findCheckFields(move);
                    }
                            measure3 += System.nanoTime() - measureTemp2;
                }
                else {
                            measureTemp2 = System.nanoTime();
                    Chessboard newBoard = board.copy();
                    ns = new Sequence(cs, move, newBoard, cs.h);
                    String nsKey=ns.toKey();
                    if(ns.len()>1&&ns.len()<=const_hm&&HM.containsKey(nsKey)&&(!use_fen || HM.get(nsKey).len()<=ns.len())){

                        measure3 += System.nanoTime() - measureTemp2;
                        count2++;
                        continue;
                    }
                    measure3 += System.nanoTime() - measureTemp2;

                    newBoard.makeMove(move);

                            measureTemp2 = System.nanoTime();

                    h = ns.evalh();
                    if (h >= 0) { //h<0 wrong moves (check)
                        if(ns.len()>1&&ns.len()<=const_hm){
                            HM.put(nsKey, ns);
                        }
                        queue.add(ns);
                    }
                            measure3 += System.nanoTime() - measureTemp2;
                }
            }
                    measure2+=System.nanoTime()-measureTemp;
        }
        nodecount+=count;
        System.out.print(count + " ");
        System.out.print(count2 + " ");
        System.out.print(measure/1000000+ " ");
        System.out.print(measure2/1000000 + " ");
        System.out.print(measure3/1000000 + " ");
        return null;
    }


    public static void main (String args[]) throws IOException {
        //Scanner sc = new Scanner(System.in);
        String fenIn;
        //fenIn = sc.nextLine();
        nodecount=0;
        for (int i = 1; i <= 60; ++i) {
            long stime = System.nanoTime();

            //read
            System.out.print(i+" ");
            String fileName = "in/" + i + ".txt";
            File file = new File(fileName);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            fenIn = br.readLine();

            //solve
            Chessboard board = Chessboard.getChessboardFromFEN(fenIn);

            /*tests
            int[][] tb = board.getBoard();
            King king = new King(4,0, 1);
            king.initIter(tb);
            int[] move = king.nextMove(tb);
            tests*/

            String result = Astar(board);

            //print
            long etime = System.nanoTime();
            System.out.print(" " + (etime - stime) / 1000000 + " ");
            if (result != null) {
                System.out.print(" S: ");
                System.out.print(result);
            }
            System.out.println();

        }
        System.out.println(nodecount);
        //sc.nextLine();
    }
}
