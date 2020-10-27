package fr.milekat.discord.utils;

import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ChunkedZippedOutputStream {
    private ZipOutputStream zipOutputStream;

    private final String path;
    private final String name;

    private long currentSize;
    private int currentChunkIndex;

    public ChunkedZippedOutputStream(String path, String name) throws FileNotFoundException {
        this.path = path;
        this.name = name;
        constructNewStream();
    }

    public void addEntry(ZipEntry entry, int splitsize) throws IOException {
        long entrySize = entry.getCompressedSize();
        if((currentSize + entrySize) > splitsize) {
            closeStream();
            constructNewStream();
        } else {
            currentSize += entrySize;
            zipOutputStream.putNextEntry(entry);
        }
    }

    private void closeStream() throws IOException {
        zipOutputStream.close();
    }

    private void constructNewStream() throws FileNotFoundException {
        zipOutputStream = new ZipOutputStream(new FileOutputStream(new File(path, constructCurrentPartName())));
        currentChunkIndex++;
        currentSize = 0;
    }

    private String constructCurrentPartName() {
        // This will give names is the form of <file_name>.part.0.zip, <file_name>.part.1.zip, etc.
        return name + ".part." + currentChunkIndex + ".zip";
    }
}