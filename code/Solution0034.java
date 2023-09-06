/**
 * @author shixuesen
 * @date 2023/6/18
 */
public class Solution0034 {
    public int[] searchRange(int[] nums, int target) {
        // 二分法查找，但是尝试用两个数绑定作为一个元素
        if (nums.length == 0) {
            return new int[]{-1, -1};
        }
        if (nums.length == 1 && nums[0] == target) {
            return new int[]{0, 0};
        }
        int left = 0, right = nums.length - 1;
        int leftIndex = -1, rightIndex = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                int subLeftIndex = - 1;
                int subRightIndex = -1;
                if (left <= mid - 1) {
                    subLeftIndex =   findLeftIndex(nums, target, left, mid - 1);

                }
                if (mid + 1 <= right) {
                    subRightIndex = findRightIndex(nums, target, mid + 1, right);
                }
                if (subLeftIndex > -1 && subLeftIndex < mid) {
                    leftIndex = subLeftIndex;
                } else {
                    leftIndex = mid;
                }
                if (subRightIndex > -1 && subRightIndex > mid) {
                    rightIndex = subRightIndex;
                } else {
                    rightIndex = mid;
                }
                return new int[]{leftIndex, rightIndex};
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
//        if (leftIndex == -1) {
//            return new int[]{-1, -1};
//        }
        return new int[]{-1, -1};
    }
    public int findLeftIndex(int[] nums, int target, int left, int right) {
        int mid = left + (right - left) / 2;
        int leftIndex = -1;
        int currentLeftIndex = -1;
        if (nums[mid] == target) {
            leftIndex = mid;
            if (leftIndex > 0) {
                if (nums[leftIndex - 1] < target) {
                    return leftIndex;
                } else {
                    currentLeftIndex = findLeftIndex(nums, target, left, mid - 1);
                    if (currentLeftIndex != -1 && currentLeftIndex < leftIndex) {
                        leftIndex = currentLeftIndex;
                    }
                }
            } else {
                return leftIndex;
            }

        } else if (nums[mid] < target && left < right) {
            return findLeftIndex(nums, target, mid + 1, right);
        } else if (left < right){
            return findLeftIndex(nums, target, left, mid - 1);
        } else {
            return leftIndex;
        }
        return leftIndex;
    }

    public int findRightIndex(int[] nums, int target, int left, int right) {
        int mid = left + (right - left) / 2;
        int rightIndex = -1;
        int currentRightIndex = -1;
        if (nums[mid] == target) {
            rightIndex = mid;
            if (rightIndex < nums.length  - 1) {
                if (nums[rightIndex + 1] > target) {
                    return rightIndex;
                } else {
                    if (mid < right) {
                        currentRightIndex = findRightIndex(nums, target, mid + 1, right);
                        if (currentRightIndex != -1 && currentRightIndex > rightIndex) {
                            rightIndex = currentRightIndex;
                        }
                    } else {
                        return rightIndex;
                    }
                }
            } else {
                return rightIndex;
            }
        } else if (nums[mid] < target && left < right && mid < right) {
            return findRightIndex(nums, target, mid + 1, right);
        } else if (left < right && mid > 0){
            return findRightIndex(nums, target, left, mid - 1);
        } else {
            return rightIndex;
        }
        return rightIndex;
    }

    public static void main(String[] args) {
        int[] nums = new int[] {1,4};
        int target = 4;
        int left = 0;
        int right = nums.length - 1;
        Solution0034 solution0034 = new Solution0034();
        int[] res = solution0034.searchRange(nums, target);
        System.out.println(res[0]);
        System.out.println(res[1]);

    }
}
