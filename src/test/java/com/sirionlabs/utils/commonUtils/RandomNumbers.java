package com.sirionlabs.utils.commonUtils;

import java.util.Random;

public class RandomNumbers {

	public static int[] getMultipleRandomNumbersWithinRangeIndex(int min, int max, int limit) {
		int size = max - min + 1;

		if (size <= limit) {
			int[] numbers = new int[size];
			numbers[0] = min;

			for (int i = 1; i < numbers.length; i++)
				numbers[i] = min + i;

			return numbers;
		}
		return (new Random().ints(min, max).distinct().limit(limit).toArray()); // here ints(min,max) => min is inclusive and max is exclusive.
	}

	public static int getRandomNumberWithinRangeIndex(int min, int max) {
		int number[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(min, max, 1);
		return number[0];
	}

	public static int[] getMultipleRandomNumbersWithinRange(int min, int max, int limit) {
		int size = max - min;

		if (size <= limit) {
			int[] numbers = new int[size];
			numbers[0] = min;

			for (int i = 1; i < numbers.length; i++)
				numbers[i] = min + i;

			return numbers;
		}
		return (new Random().ints(min, max).distinct().limit(limit).toArray());
	}

	public static int getRandomNumberWithinRange(int min, int max) {
		int number[] = RandomNumbers.getMultipleRandomNumbersWithinRange(min, max, 1);
		return number[0];
	}
}
