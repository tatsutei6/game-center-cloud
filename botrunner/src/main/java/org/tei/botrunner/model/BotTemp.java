package org.tei.botrunner.model;

import java.util.ArrayList;
import java.util.List;

public class BotTemp implements org.tei.botrunner.model.BotInterface {
    static class Cell {
        public int row, column;

        public Cell(int _row, int _column) {
            this.row = _row;
            this.column = _column;
        }
    }

    private boolean isTailExpand(int step) {  // 检验当前回合，蛇的长度是否增加
        if (step <= 10) return true;
        return step % 3 == 1;
    }

    public List<Cell> getCells(int row, int column, String steps) {
        steps = steps.substring(1, steps.length() - 1);
        List<Cell> res = new ArrayList<>();

        int[] dRows = {-1, 0, 1, 0};
        int[] dColumns = {0, 1, 0, -1};
        int _row = row, _column = column;
        int step = 0;
        res.add(new Cell(_row, _column));
        for (int i = 0; i < steps.length(); i++) {
            int d = steps.charAt(i) - '0';
            _row += dRows[d];
            _column += dColumns[d];
            res.add(new Cell(_row, _column));
            if (!isTailExpand(++step)) {
                res.remove(0);
            }
        }
        return res;
    }

    @Override
    public Integer nextMove(String input) {
        String[] strArr = input.split("#");
        int[][] g = new int[13][14];
        for (int i = 0, k = 0; i < 13; i++) {
            for (int j = 0; j < 14; j++, k++) {
                if (strArr[0].charAt(k) == '1') {
                    g[i][j] = 1;
                }
            }
        }

        int aRow = Integer.parseInt(strArr[1]);
        int aColumn = Integer.parseInt(strArr[2]);
        int bRow = Integer.parseInt(strArr[4]);
        int bColumn = Integer.parseInt(strArr[5]);

        List<Cell> aCells = getCells(aRow, aColumn, strArr[3]);
        List<Cell> bCells = getCells(bRow, bColumn, strArr[6]);

        for (Cell c : aCells) g[c.row][c.column] = 1;
        for (Cell c : bCells) g[c.row][c.column] = 1;

        int[] dRows = {-1, 0, 1, 0};
        int[] dColumns = {0, 1, 0, -1};
        for (int i = 0; i < 4; i++) {
            int x = aCells.get(aCells.size() - 1).row + dRows[i];
            int y = aCells.get(aCells.size() - 1).column + dColumns[i];
            if (x >= 0 && x < 13 && y >= 0 && y < 14 && g[x][y] == 0) {
                return i;
            }
        }
        return 0;
    }
}
