package org.tei.gamecenter.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private Integer id;
    // -1为亲自玩，其他为bot id
    private Integer botId;
    private String botCode;
    // row : sx，初始行位置
    private Integer row;
    // column : sy，初始列位置
    private Integer column;
    private List<Integer> steps;


    /**
     * 判断当前回合，蛇的body是否增加
     * @param step
     * @return
     */
    private boolean isExpandTail(int step) {
        if (step <= 10) return true;
        return step % 3 == 1;
    }

    public List<Cell> getCells() {
        List<Cell> cells = new ArrayList<>();

        int[] dRows = {-1, 0, 1, 0}, dColumns = {0, 1, 0, -1};
        int _row = row, _column = column;
        int step = 0;
        cells.add(new Cell(_row, _column));
        // 根据steps，计算蛇的身体
        for (int d : steps) {
            _row += dRows[d];
            _column += dColumns[d];
            cells.add(new Cell(_row, _column));
            step++;
            if (!isExpandTail(step)) {
                // 如果蛇尾不增加，将蛇尾删掉
                cells.remove(0);
            }
        }
        return cells;
    }

    public String getStepsString() {
        StringBuilder res = new StringBuilder();
        for (int d : steps) {
            res.append(d);
        }
        return res.toString();
    }
}
