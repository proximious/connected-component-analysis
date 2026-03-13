import java.util.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import javax.imageio.ImageIO;


public class CCA {
    public static final String ERR_STRING = "Could not load the input image";
    public static final String DASH = "----------";
    /**
     * Loads an image from the specified file and converts it to a 2D boolean array.
     * Each pixel is converted to true if its red channel value is greater than 128,
     * otherwise false. Only the red channel is used for the conversion.
     *
     * @param filename the path to the image file to load
     * @return a 2D boolean array representing the image, where true indicates a
     *         bright pixel and false indicates a dark pixel; returns null if the
     *         file cannot be read or is invalid
     */
	public static boolean[][] loadImage(String filename){
		File f = new File(filename);
		try {
			BufferedImage img_buff = ImageIO.read(f);
			Raster raster = img_buff.getData();
			
			int h = img_buff.getHeight();
			int w = img_buff.getWidth();
			int[] pixel = new int[3];
			boolean[][] img_bool = new boolean[h][w];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					raster.getPixel(x, y, pixel);
					img_bool[y][x] = pixel[0] > 128;					 
				}
			}
			
			return img_bool;
		} catch (Exception _) {
			System.out.println("Invalid image file");
			return new boolean[0][0];
		}		
	}
	
	/**
     * Converts a 2D boolean image array into a string representation.
     * Each true value is represented as '#' and each false value as '-'.
     * Rows are separated by newline characters.
     *
     * @param img_bool the 2D boolean array representing an image
     * @return a string where each row corresponds to a row in the image,
     *         with '#' for true pixels and '-' for false pixels
     */
	public static String boolImgToString(boolean[][] img_bool) {
		StringBuffer buffer = new StringBuffer();
		for (int y = 0; y < img_bool.length; y++) {
			for (int x = 0; x < img_bool[y].length; x++) {
				buffer.append(img_bool[y][x] ? "#" : "-");
			}
			buffer.append("\n");
		}
		
		return buffer.toString();
	}

    public record Pair<T, U>(T first, U second) { }

    /**
     * Converts a boolean image array into a union-find structure and a list of
     * contiguous pixel groups.
     * Each true pixel is treated as part of a connected component. Neighboring true
     * pixels
     * (up and left) are connected in a QuickUnion structure, and contiguous pixels
     * in each
     * row are collected into lists.
     *
     * @param imgBoolean the 2D boolean array representing an image
     * @return a Pair containing:
     *         - a QuickUnion object representing connected components of true
     *         pixels
     *         - a List of Lists of integers, where each inner list contains the
     *         linear indices
     *         of contiguous true pixels in a row
     */
    public static Pair < QuickUnion, List<List<Integer>>> img2UF(boolean [][] imgBoolean){
        // get values for quickunion constructor
        int rows = imgBoolean.length;
        int cols = imgBoolean[0].length;
        int size = rows * cols;

        // make objects to return later
        QuickUnion quick_union = new QuickUnion(size);
        List<List<Integer>> contiguous_pixels = new ArrayList<>();

        // iterate over all the rows 
        for (int row = 0; row < rows; row++) {
            // get the row's pixels that we will try to merge later
            List<Integer> row_pixel = new ArrayList<>();

            // iterate through all the columns
            for (int col = 0; col < cols; col++) {
                // only care about positions that are True, ignore False
                if (!imgBoolean[row][col]) {
                    // if we found True pixels in the row
                    // dump the row into contiguous pixels and reset row_pixel
                    if (!row_pixel.isEmpty()) {
                      contiguous_pixels.add(row_pixel);
                      row_pixel = new ArrayList<>();
                    }
                    // continue regardless if we dump values or not
                    continue;
                }

                // find the position of the current row/col value
                int val = (row * cols) + col;
                
                // found a True pixel
                row_pixel.add(val);

                // find the indeces of its neighbors
                int up = ((row - 1) * cols) + col;

                int left = (row * cols) + (col - 1);

                // check the up neighbor
                if (row > 0 && imgBoolean[row - 1][col]){
                    quick_union.union(val, up);
                }

                // check the left neighbor
                if (col > 0 && imgBoolean[row][col - 1]) {
                    quick_union.union(val, left);
                }

            }
            // if we reach the end of the row, dump the rest of the row into contiguous pixels
            if (!row_pixel.isEmpty()) {
                contiguous_pixels.add(row_pixel);
            }
        }

        // combine the two structures into a pair
        return new Pair<>(quick_union, contiguous_pixels);
    }

    /**
     * Generates complete connected components (CCs) from a QuickUnion structure and
     * initial row-based pixel groups.
     * Takes the raw contiguous pixel lists generated from img2UF() and merges any
     * lists whose elements
     * are connected according to the QuickUnion, producing full connected
     * components across the image.
     *
     * @param imgUF the QuickUnion object representing connected pixels in the image
     * @param rawCC the initial List of Lists of integers, each representing
     *              contiguous true pixels in a row
     * @return a List of Lists of integers, where each inner list contains all pixel
     *         indices
     *         that belong to the same connected component
     */
    public static List<List<Integer>> genCC(QuickUnion imgUF,  List<List<Integer>> rawCC){
        // loop through each index in rawCC, double for loop
        // if i and j's first elements are connected, add all of j to im then remove j from rawCC

        // two pointer approach
        // compare every element with every other element later than it
        for (int i = 0; i < rawCC.size(); i++) {
            List<Integer> L1 = rawCC.get(i);

            // when using a for loop, warning came up, changed to while loop
            // start at the point after L1
            int j = i + 1;
            while (j < rawCC.size()) {
                List<Integer> L2 = rawCC.get(j);

                // select first element from each list
                int L1_first = L1.get(0);
                int L2_first = L2.get(0);

                // use the quickunion data structure to see if they are connected
                boolean connected = imgUF.connected(L1_first, L2_first);

                // if connected, we have to merge L1 and L2 into a single larger list
                if (connected) {
                    // append all elements from L2 to 
                    L1.addAll(L2);
                    // then remove L2 from the rawCC
                    rawCC.remove(L2);
                } else {
                    // when we do not remove something from rawCC, we have to increment j
                    j++;
                }
            }
        }

        return rawCC;
    }

    /**
     * Separates connected components (CCs) into triangles and rectangles based on
     * their bounding box density.
     * For each CC, computes the bounding box, calculates the ratio of pixels in the
     * CC to the bounding box area,
     * and classifies it as a rectangle if the ratio meets or exceeds the threshold,
     * or a triangle otherwise.
     *
     * @param imgH      the height of the image in pixels
     * @param imgW      the width of the image in pixels
     * @param CCs       a List of Lists of integers representing connected
     *                  components, where each integer is a pixel index
     * @param threshold the area ratio threshold used to distinguish rectangles from
     *                  triangles
     * @return a Pair containing:
     *         - the first list of integers with indices of CCs classified as
     *         triangles
     *         - the second list of integers with indices of CCs classified as
     *         rectangles
     */
    public static Pair<List<Integer>, List<Integer>> separateCC(int imgH, int imgW, List<List<Integer>> CCs, double threshold){
        // the first list: list of indices (like the indices in the blue list in figure 3.b) of CCs that belongs to triangle
        // the second list: list of indices (like the indices in the blue list in figure 3.b) of CCs that belongs to rectangle
        List<Integer> triangles = new ArrayList<>();
        List<Integer> rectangles = new ArrayList<>();

        // go over each connected component
        for (int i = 0; i < CCs.size(); i++) {
            
            // get the component
            List<Integer> component = CCs.get(i);
            
            // find the bounding box for the CC where:
            // top_row = smallest row
            // bottom_row = largest row
            // left_col = smallest col
            // right_col = largest col
            int top_row = imgH;
            int bottom_row = 0;
            int left_col = imgW;
            int right_col = 0;

            // loop over all pixels in this CC
            for (int j = 0; j < component.size(); j++) {
                // find the pixel index in this CC
                int pixel_index = component.get(j);

                // find the row and col for this CC's pixel
                int pixel_row = pixel_index / imgW;
                int pixel_col = pixel_index % imgW;

                // find new top row for bounding box
                if (pixel_row < top_row) {
                    top_row = pixel_row;
                }

                // find new bottom row for bounding box
                if (pixel_row > bottom_row) {
                    bottom_row = pixel_row;
                }

                // find new left col for bounding box
                if (pixel_col < left_col) {
                    left_col = pixel_col;
                }

                // find new right col for bounding box
                if (pixel_col > right_col) {
                    right_col = pixel_col;
                }
            }

            // find dimensions of bounding box
            int bounding_box_height = bottom_row - top_row;
            int bounding_box_width = right_col - left_col;

            // calculate the total area possible for this CC
            int bounding_box_area = bounding_box_height * bounding_box_width;

            // find the total number of pixels this CC occupates
            int shape_area = component.size();

            // find the ratio of the area of space this CC takes up
            double area_ratio = (double) shape_area / bounding_box_area;
            
            // determine whether this CC is a rectangle or triangle
            if (area_ratio >= threshold) {
                rectangles.add(i);
            } else {
                triangles.add(i);
            }
        }
        // return both as a pair
        return new Pair<>(triangles, rectangles);
    }

    /**
     * Performs data collection and connected component analysis on an array of
     * image files.
     * For each image, it loads the image, generates connected components,
     * classifies each component
     * as a triangle or rectangle, and measures the runtime of the analysis over 5
     * iterations.
     * Results including total and average run time, number of shapes, and pixel
     * count are printed to the console.
     *
     * @param test_images an array of File objects representing the images to
     *                    analyze
     */
    public static void dataCollection(File[] test_images){
        for (File file : test_images) {
            // pretty much use most of main method in here
            boolean[][] img = loadImage(file.getPath());
            if (img.length == 0) {
                System.out.println(ERR_STRING);
                return;
            }
            
            // declare everything out of the loops for the prints afterwards
            double total_time = 0;
            Pair<QuickUnion, List<List<Integer>>> result = null;
            List<List<Integer>> CCs = null;
            int components = 0;
            int imgH = 0;
            int imgW = 0;
            Pair<List<Integer>, List<Integer>> separatedCCs = null;
            List<Integer> triangles = new ArrayList<>();
            List<Integer> rectangles = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                // take the start time for the CCA analysis
                long start = System.nanoTime();

                // get the contiguous list and quick union structure from the image
                result = img2UF(img);
                
                // create the connected components
                CCs = genCC(result.first(), result.second());
                components = CCs.size();
                
                // then separate all of them to classify each as rectangle or triangle
                imgH = img.length;
                imgW = img[0].length;
                separatedCCs = separateCC(imgH, imgW, CCs, 0.75);
                triangles = separatedCCs.first();
                rectangles = separatedCCs.second();

                // get the end time for the CCA analysis
                long end = System.nanoTime();

                total_time += (end - start) / 1_000_000_000.0;
                
            }

            // print this only after last run is done
            // JUST FOR FULL ANALYSIS
            // print out all info for this file
            System.out.println("Name of file: \t\t" + file.getName());

            System.out.printf("Total run time (s):\t%.6f\n", total_time);
            double average_time = total_time / 5.0;
            System.out.printf("Average run time (s):\t%.6f\n", average_time);

            System.out.println("Total shapes : \t\t" + components);
            System.out.println("Number of triangles: \t" + triangles.size());
            System.out.println("Number of rectangles: \t" + rectangles.size());

            System.out.println("Number of pixels: \t" + (imgH * imgW));

            System.out.println(DASH);
        }
    }

    /**
     * Performs connected component analysis on images with a specific total pixel
     * count and collects timing statistics.
     * For each image, it loads the image, generates connected components,
     * classifies them as triangles or rectangles,
     * and records the runtime if the image has the specified number of pixels.
     * After processing all images, it prints
     * the minimum, maximum, total, average, and median run times for the matching
     * images.
     *
     * @param test_images an array of File objects representing the images to
     *                    analyze
     * @param num_pixels  the specific number of pixels an image must have to be
     *                    included in the timing statistics
     */
    public static void dataCollectionUniqueResolution (File[] test_images, int num_pixels) {
        // declare everything out of the loops for the prints afterwards
        Pair<QuickUnion, List<List<Integer>>> result = null;
        List<List<Integer>> CCs = null;
        int imgH = 0;
        int imgW = 0;
        int size = 0;
        Pair<List<Integer>, List<Integer>> separatedCCs = null;

        ArrayList<Double> time_pixels = new ArrayList<>();

        for (File file : test_images) {
            // pretty much use most of main method in here
            boolean[][] img = loadImage(file.getPath());
            if (img == null) {
                System.out.println(ERR_STRING);
                return;
            }

            for (int i = 0; i < 5; i++) {
                // take the start time for the CCA analysis
                long start = System.nanoTime();

                // get the contiguous list and quick union structure from the image
                result = img2UF(img);

                // create the connected components
                CCs = genCC(result.first(), result.second());

                // then separate all of them to classify each as rectangle or triangle
                imgH = img.length;
                imgW = img[0].length;
                size = imgH * imgW;

                
                separatedCCs = separateCC(imgH, imgW, CCs, 0.75);
                
                // get the end time for the CCA analysis
                long end = System.nanoTime();
                
                // check if we can store this time
                if (size != num_pixels) {
                    continue;
                }

                time_pixels.add((end - start) / 1_000_000.0);
            }
        }
        // remove warning
        separatedCCs.first();
        
        // find the information needed for the print statements at the end
        double min = Collections.min(time_pixels);

        double max = Collections.max(time_pixels);

        double sum = 0;
        for (double t : time_pixels) {
            sum += t;
        }
        double avg = sum / time_pixels.size();

        Collections.sort(time_pixels);
        double median;
        int n = time_pixels.size();
        if (n % 2 == 0) {
            median = (time_pixels.get(n/2 - 1) + time_pixels.get(n/2)) / 2.0;
        } else {
            median = time_pixels.get(n/2);
        }
        
        System.out.println("Number of pixels: \t" + num_pixels);
        System.out.printf("Min time (ms):\t\t%.6f\n", min);
        System.out.printf("Max time (ms):\t\t%.6f\n", max);
        System.out.printf("Total run time (ms):\t%.6f\n", sum);
        System.out.printf("Average run time (ms):\t%.6f\n", avg);
        System.out.printf("Median time (ms):\t%.6f\n", median);

        System.out.println(DASH);
    }

    /**
     * Performs connected component analysis on images with a specific number of
     * connected components (CCs) and collects timing statistics.
     * For each image, it loads the image, generates connected components,
     * classifies them as triangles or rectangles,
     * and records the runtime if the number of CCs matches the specified value.
     * After processing all images, it prints
     * the minimum, maximum, total, average, and median run times for the matching
     * images.
     *
     * @param test_images an array of File objects representing the images to
     *                    analyze
     * @param num_cc      the specific number of connected components an image must
     *                    have to be included in the timing statistics
     */
    public static void dataCollectionUniqueCC(File[] test_images, int num_cc) {
        // declare everything out of the loops for the prints afterwards
        Pair<QuickUnion, List<List<Integer>>> result = null;
        List<List<Integer>> CCs = null;
        int imgH = 0;
        int imgW = 0;
        Pair<List<Integer>, List<Integer>> separatedCCs = null;
        int components = 0;

        ArrayList<Double> time_cc = new ArrayList<>();

        for (File file : test_images) {
            // pretty much use most of main method in here
            boolean[][] img = loadImage(file.getPath());
            if (img == null) {
                System.out.println(ERR_STRING);
                return;
            }

            for (int i = 0; i < 5; i++) {
                // take the start time for the CCA analysis
                long start = System.nanoTime();

                // get the contiguous list and quick union structure from the image
                result = img2UF(img);

                // create the connected components
                CCs = genCC(result.first(), result.second());
                components = CCs.size();

                // then separate all of them to classify each as rectangle or triangle
                imgH = img.length;
                imgW = img[0].length;
                
                separatedCCs = separateCC(imgH, imgW, CCs, 0.75);
                
                // get the end time for the CCA analysis
                long end = System.nanoTime();
                
                if (components != num_cc) {
                    continue;
                }

                time_cc.add((end - start) / 1_000_000.0);

            }
        }
        // remove warning
        separatedCCs.first();
        
        // find the information needed for the print statements at the end
        double min = Collections.min(time_cc);

        double max = Collections.max(time_cc);

        double sum = 0;
        for (double t : time_cc) {
            sum += t;
        }
        double avg = sum / time_cc.size();

        Collections.sort(time_cc);
        double median;
        int n = time_cc.size();
        if (n % 2 == 0) {
            median = (time_cc.get(n / 2 - 1) + time_cc.get(n / 2)) / 2.0;
        } else {
            median = time_cc.get(n / 2);
        }

        System.out.println("Number of CCs:\t\t" + num_cc);
        System.out.printf("Min time (ms):\t\t%.6f\n", min);
        System.out.printf("Max time (ms):\t\t%.6f\n", max);
        System.out.printf("Total run time (ms):\t%.6f\n", sum);
        System.out.printf("Average run time (ms):\t%.6f\n", avg);
        System.out.printf("Median time (ms):\t%.6f\n", median);

        System.out.println(DASH);

    }
	
    /**
     * Main program entry point with a menu-driven interface for analyzing images or
     * running data collection.
     * Users can choose to analyze a single image, perform data collection on all
     * test images, or exit the program.
     * The single image mode loads the image, generates connected components,
     * classifies them as triangles or rectangles,
     * and prints the results. The data collection mode runs multiple experiments
     * including raw data collection,
     * unique resolution timing, and unique connected component timing for
     * pre-defined image sets.
     *
     * @param args command-line arguments (not used)
     */
    public static void main() {
		try (Scanner scanner = new Scanner(System.in)) {
            // get the file
            System.out.println(new File(".").getAbsolutePath());
            
            // prompt a continuous input from the user
            while (true) {
                System.out.println("Menu: ");
                System.out.println("\tsf : single file");
                System.out.println("\tdc : data collection");
                System.out.println("\texit : quit program");

                String choice = scanner.nextLine().trim().toLowerCase();
                
                // Analyze One Image Mode
                if (choice.equals("sf")) {
                    System.out.print("Enter image file name: ");
                    String user_file = scanner.nextLine();

                    boolean[][] img = loadImage("test_images/" + user_file);
                    if (img.length == 0){
                        System.out.println(ERR_STRING);
                        return;
                    }
                    if (img.length <= 100) {
                        System.out.println(boolImgToString(img));
                    }
            
                    Pair<QuickUnion, List<List<Integer>>> result = img2UF(img);
            
                    List<List<Integer>> CCs = genCC(result.first(), result.second());
                    System.out.println("Total shapes : " + CCs.size());
            
                    int imgH = img.length;
                    int imgW = img[0].length;
                    Pair<List<Integer>, List<Integer>> separatedCCs = separateCC(imgH, imgW, CCs, 0.75);
                    List<Integer> triangles = separatedCCs.first();
                    List<Integer> rectangles = separatedCCs.second();
            
                    System.out.println("Number of triangles: " + triangles.size());
                    System.out.println("Number of rectangles: " + rectangles.size());
                }

                // Data Collection Mode
                else if (choice.equals("dc")) {
                    File f = new File("test_images/");
                    File [] files = f.listFiles();
                    // run data collection for all the files at once
                    System.out.println("-------------------");
                    System.out.println("RAW DATA COLLECTION");
                    System.out.println("-------------------");
                    dataCollection(files);

                    // run it for each unique resolution of R
                    System.out.println("-----------------");
                    System.out.println("UNIQUE RESOLUTION");
                    System.out.println("-----------------");
                    dataCollectionUniqueResolution(files, 10000);
                    dataCollectionUniqueResolution(files, 20000);
                    dataCollectionUniqueResolution(files, 40000);
                    dataCollectionUniqueResolution(files, 80000);
                    dataCollectionUniqueResolution(files, 160000);
                    dataCollectionUniqueResolution(files, 320000);
                    dataCollectionUniqueResolution(files, 640000);

                    // run it for each unique connected components of M
                    System.out.println("---------------------------");
                    System.out.println("UNIQUE CONNECTED COMPONENTS");
                    System.out.println("---------------------------");
                    dataCollectionUniqueCC(files, 4);
                    dataCollectionUniqueCC(files, 8);
                    dataCollectionUniqueCC(files, 16);
                    dataCollectionUniqueCC(files, 32);
                    dataCollectionUniqueCC(files, 64);
                }
            
                else if (choice.equals("exit")) {
                    System.out.println("Exiting program");
                    break;
                }

                else {
                    System.out.println("Invalid command");
                }
            }
        }
	}

}
