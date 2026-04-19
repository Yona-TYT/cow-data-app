package com.example.cow_data.drive;

public class DriveFileMeta {

    public final String id;
    public final String name;
    public final String md5Checksum;
    public final String modifiedTime;

    public DriveFileMeta(String id, String name, String md5Checksum, String modifiedTime) {
        this.id = id;
        this.name = name;
        this.md5Checksum = md5Checksum;
        this.modifiedTime = modifiedTime;
    }

    public boolean hasMd5() {
        return md5Checksum != null && !md5Checksum.isEmpty();
    }

    public boolean hasModifiedTime() {
        return modifiedTime != null && !modifiedTime.isEmpty();
    }

    @Override
    public String toString() {
        return "DriveFileMeta{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", md5Checksum='" + md5Checksum + '\'' +
                ", modifiedTime='" + modifiedTime + '\'' +
                '}';
    }
}