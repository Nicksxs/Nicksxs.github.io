public class Solution16 {

  public int threeSumClosest(int[] nums, int target) {
    Arrays.sort(nums);
    // 当前最近的和
    int closestSum = nums[0] + nums[1] + nums[nums.length - 1];
    for (int i = 0; i < nums.length - 2; i++) {
      if (i == 0 || nums[i] != nums[i - 1]) {
        // 左指针
        int left = i + 1;
        // 右指针
        int right = nums.length - 1;
        // 判断是否遍历完了
        while (left < right) {
          // 当前的和
          int sum = nums[i] + nums[left] + nums[right];
          // 小优化，相等就略过了
          while (left < right && nums[left] == nums[left + 1]) {
            left++;
          }
          while (left < right && nums[right] == nums[right - 1]) {
            right--;
          }
          // 这里判断，其实也还是希望趋近目标值
          if (sum < target) {
            left++;
          } else {
            right--;
          }
          // 判断是否需要替换
          if (Math.abs(sum - target) < Math.abs(closestSum - target)) {
            closestSum = sum;
          }
        }
      }
    }
    return closestSum;
  }
}
