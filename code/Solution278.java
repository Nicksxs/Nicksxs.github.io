public class Solution278 {

  public int firstBadVersion(int n) {
    // 类似于双指针法
    int left = 1, right = n, mid;
    while (left < right) {
      // 取中点
      mid = left + (right - left) / 2;
      // 如果不是错误版本，就往右找
      if (!isBadVersion(mid)) {
        left = mid + 1;
      } else {
        // 如果是的话就往左查找
        right = mid;
      }
    }
    // 这里考虑交界情况是，在上面循环中如果 left 是好的，right 是坏的，那进入循环的时候 mid == left
    // 然后 left = mid + 1 就会等于 right，循环条件就跳出了，此时 left 就是那个起始的错误点了
    // 其实这两个是同一个值
    return left;
  }
}
