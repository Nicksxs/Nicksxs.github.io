public class Solution885 {

  public int[][] spiralMatrixIII(int rows, int cols, int rStart, int cStart) {
    int size = rows * cols;
    int x = rStart, y = cStart;
    // 返回的二维矩阵
    int[][] matrix = new int[size][2];
    // 传入的参数就是入口第一个
    matrix[0][0] = rStart;
    matrix[0][1] = cStart;
    // 作为数量
    int z = 1;
    // 步进，1，1，2，2，3，3，4 ... 螺旋矩阵的增长
    int a = 1;
    // 方向 1 表示右，2 表示下，3 表示左，4 表示上
    int dir = 1;
    while (z < size) {
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < a; j++) {
          // 处理方向
          if (dir % 4 == 1) {
            y++;
          } else if (dir % 4 == 2) {
            x++;
          } else if (dir % 4 == 3) {
            y--;
          } else {
            x--;
          }
          // 如果在实际矩阵内
          if (x < rows && y < cols && x >= 0 && y >= 0) {
            matrix[z][0] = x;
            matrix[z][1] = y;
            z++;
          }
        }
        // 转变方向
        dir++;
      }
      // 步进++
      a++;
    }
    return matrix;
  }
}
