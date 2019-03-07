import java.util.*;

class Piecet{
    int x = 0,y = 0;  //board[y][x] = (x,y) CUZ BOARD IS TILTED
    int[][] directions = new int[][]{};
    boolean scalable = false;
    int it, scale;

    void initIter(){ //initiate iterator
        it=0;
        scale=1;
    }

    int[] nextMove(){ //returns (y,x) cuz board is oriented WRONG
        if(it<directions.length){
            int[] r=new int[]{y,x};

            r[0]+=scale*directions[it][0];
            r[1]+=scale*directions[it][1];

            if(scalable){
                scale++;
                if(y+scale*directions[it][0] > 7 || y+scale*directions[it][0] < 0
                        || x+scale*directions[it][1] < 0 || x+scale*directions[it][1] > 7) { //if out of board
                    scale = 1;
                    it++;
                }
            }
            else it++;

            while(it<directions.length &&
                    (y+scale*directions[it][0] > 7 || y+scale*directions[it][0] < 0
                            || x+scale*directions[it][1] < 0 || x+scale*directions[it][1] > 7)) { //if out of board
                it++;
            }

            return r;
        }
        return null;
    }
}

class Kingt extends Piecet{
    Kingt(int x_in, int y_in){
        x=x_in;
        y=y_in;
        directions = new int[][]{{1, 1}, {-1, 1}, {-1, -1}, {1, -1}, {1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        scalable = false;
        initIter();
    }
}

public class HelloWorld {
    private static class Stats{
        public static Kingt k;

        Stats(){}


    }

    public static void main(String args[]){
        Stats a=new Stats();
        a.k=new Kingt(1,1);
        Stats b=new Stats();
        System.out.println(b.k.x);
        b.k=new Kingt(2,2);
        System.out.println(a.k.y);
    }
}
