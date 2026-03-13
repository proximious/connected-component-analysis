# Connected Component Analysis on Binary Images

## Overview

This project implements Connected Component Analysis (CCA) on binary images using the Union Find data structure. The program loads binary images, detects connected groups of foreground pixels, and classifies each detected component as either a triangle or a rectangle.

The goal of the project is to combine image processing concepts with data structures such as Union Find and dynamic lists.

## Project Structure

```
CONNECTED-COMPONENT-ANALYSIS/
│
├── .vscode/
│   VS Code configuration files
│
├── bin/
│   Compiled Java class files
│
├── lib/
│   External libraries (if used)
│
├── src/
│   ├── CCA.java
│   │   Main program containing the connected component logic,
│   │   shape classification, and program execution.
│   │
│   └── QuickUnion.java
│       Implementation of the Union Find data structure with
│       weighted union and path compression.
│
├── test_images/
│   Directory containing binary test images used for testing
│   and data collection.
│
└── README.md
    Project documentation
```

## How the Program Works

### 1. Image Loading

Binary images are loaded and converted into a `boolean[][]` representation:

- `true` represents foreground pixels
- `false` represents background pixels

This representation makes it easier to analyze the structure of the image.

### 2. Mapping Pixels to Union Find

Each pixel in the image is mapped from a 2D coordinate to a 1D index:

```
index = row * width + col
```

This allows the image pixels to be stored in the Union Find structure.

### 3. Detecting Connected Components

Foreground pixels are connected with their neighbors using Union Find operations.  
Only **up** and **left** neighbors are checked to avoid duplicate connections.

The algorithm builds lists of contiguous pixels and merges them when they belong to the same connected component.

The result is a list of lists where each inner list represents one connected component.

### 4. Shape Classification

Each connected component is classified by comparing its area to the area of its bounding box.

```
ratio = component_area / bounding_box_area
```

Decision rule:

- ratio ≥ 0.75 → rectangle
- ratio < 0.75 → triangle

### 5. Data Collection Mode

The program can also run performance tests across multiple images in the `test_images` directory.

For each image:

1. The image is loaded
2. Connected component analysis runs 5 times
3. The average runtime is recorded

Statistics collected include:

- number of pixels (image resolution)
- number of connected components
- runtime performance

## Compilation

Compile the project from the root directory:

```bash
javac -d bin src/*.java
```

## Running the Program

Run the program using:

```bash
java -cp bin CCA
```

Make sure the `test_images` folder is located in the project root directory.

## Example Output

```
Total shapes: 4
Number of triangles: 1
Number of rectangles: 3
```

## Concepts Used

- Union Find (Quick Union)
- Weighted Union
- Path Compression
- Connected Component Analysis
- Bounding Box Computation
- Runtime Performance Analysis
