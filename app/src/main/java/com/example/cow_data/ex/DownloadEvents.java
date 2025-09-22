package com.example.cow_data.ex;

import java.util.ArrayList;

public class DownloadEvents {

        @SuppressWarnings("unchecked")
        public static abstract class BaseDownloadEvent implements java.io.Serializable{
            public boolean success;
            public String message;
            public Throwable throwable;
            public int count = 0;


            /**
             * Convenience function, returns a succeeded event
             */
            public <T extends BaseDownloadEvent> T succeeded(){
                this.success = true;
                return (T) this;
            }

            /**
             * Convenience function, returns a success event with a message
             */
            public <T extends BaseDownloadEvent> T succeeded(String message){
                this.success = true;
                this.message = message;
                return (T) this;
            }

            /**
             * Convenience function, returns a success event with a message and count
             */
            public <T extends BaseDownloadEvent> T succeeded(String message, int count){
                this.success = true;
                this.message = message;
                this.count = count;
                return (T) this;
            }


            /**
             * Convenience function, returns a failed event
             */
            public <T extends BaseDownloadEvent> T failed(){
                this.success = false;
                this.message = null;
                this.throwable = null;
                return (T)this;
            }

            /**
             * Convenience function, returns a failed event with just a message
             */
            public <T extends BaseDownloadEvent> T failed(String message){
                this.success = false;
                this.message = message;
                this.throwable = null;
                return (T)this;
            }

            /**
             * Convenience function, returns a failed event with a message and a throwable
             */
            public <T extends BaseDownloadEvent> T failed(String message, Throwable throwable){
                this.success = false;
                this.message = message;
                this.throwable = throwable;
                return (T)this;
            }
        }

        public static class AutoEmail extends BaseDownloadEvent {
            public ArrayList<String> smtpMessages;

        }


        public static class CustomUrl extends com.example.cow_data.ex.UploadEvents.BaseUploadEvent {}

        public static class Dropbox extends com.example.cow_data.ex.UploadEvents.BaseUploadEvent {}
        public static class GoogleDrive extends com.example.cow_data.ex.UploadEvents.BaseUploadEvent {}

        public static class Ftp extends com.example.cow_data.ex.UploadEvents.BaseUploadEvent {
            public ArrayList<String> ftpMessages;
        }

        public static class OpenGTS extends com.example.cow_data.ex.UploadEvents.BaseUploadEvent {}

        public static class OpenStreetMap extends com.example.cow_data.ex.UploadEvents.BaseUploadEvent {}

        public static class OwnCloud extends com.example.cow_data.ex.UploadEvents.BaseUploadEvent {}

        public static class SFTP extends com.example.cow_data.ex.UploadEvents.BaseUploadEvent {
            public String fingerprint;
            public String hostKey;
        }
    }


