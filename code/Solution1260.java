public class Solution1260 {

  public List<List<Integer>> shiftGrid(int[][] grid, int k) {
    // 行数
    int m = grid.length;
    // 列数
    int n = grid[0].length;
    // 偏移值，取下模
    k = k % (m * n);
    // 反向取下数量，因为我打算直接从头填充新的矩阵
    /*
     *    比如
     *    1 2 3
     *    4 5 6
     *    7 8 9
     *    需要变成
     *    9 1 2
     *    3 4 5
     *    6 7 8
     *    就要从 9 开始填充
     */
    int reverseK = m * n - k;
    List<List<Integer>> matrix = new ArrayList<>();
    // 这类就是两层循环
    for (int i = 0; i < m; i++) {
      List<Integer> line = new ArrayList<>();
      for (int j = 0; j < n; j++) {
        // 数量会随着循环迭代增长, 确认是第几个
        int currentNum = reverseK + i * n + (j + 1);
        // 这里处理下到达矩阵末尾后减掉 m * n
        if (currentNum > m * n) {
          currentNum -= m * n;
        }
        // 根据矩阵列数 n 算出在原来矩阵的位置
        int last = (currentNum - 1) % n;
        int passLine = (currentNum - 1) / n;

        line.add(grid[passLine][last]);
      }
      matrix.add(line);
    }
    return matrix;
  }
}
