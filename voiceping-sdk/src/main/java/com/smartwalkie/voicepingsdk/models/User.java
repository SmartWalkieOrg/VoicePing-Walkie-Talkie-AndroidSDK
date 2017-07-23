package com.smartwalkie.voicepingsdk.models;



public class User {

    private static final String TAG = User.class.getSimpleName();
    public static final String DEFAULT_DISPLAY_NAME_PREFIX = "User_";

    public static final String DB_FIELD_ID          = "user_id";
    public static final String DB_USERNAME          = "_username";
    public static final String DB_EMAIL             = "_email";
    public static final String DB_USER_ID           = "_userId";
    public static final String DB_FAVORITE          = "_favorite";
    public static final String DB_UUID              = "_uuid";
    public static final String DB_SOCKET_URL        = "_socket_url";
    public static final String DB_privilege         = "_privilege";
    public static final String DB_AVATAR_URL        = "_avatar_url";
    public static final String DB_STATUS            = "_status";
    public static final String DB_FULLNAME          = "_fullname";
    public static final String DB_PHONE             = "_phone";
    public static final String DB_SHOW_IN_CONTACT   = "_showcontact";
    public static final String DB_COMPANY           = "_company";

    int id;
    String username;
    String email;
    String uuid;
    String socket_url;
    String privilege;
    String avatar_url;
    boolean isFavoritel;
    int statusValue;

    String phone;

    String fullname;
    boolean showContact;
    String company;


    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFullName() {
        return fullname;
    }

    public void setFullName(String fullName) {
        this.fullname = fullName;
    }

    public User(){
    }

    public boolean isFavorite() {
        return isFavoritel;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavoritel = isFavorite;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
//        if(username==null){
//            username="null"; //for testing
//        }
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return getDisplayName() +" "+id;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof User)){
            return false;
        }
        User user=(User)o;
        return user.getId()==id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSocket_url() {
        return socket_url;
    }

    public void setSocket_url(String socket_url) {
        this.socket_url = socket_url;
    }

    public String getPrivilege() {
        return privilege;
    }

    public void setPrivilege(String privilege) {
        this.privilege = privilege;
    }

    public String getAvatar_url() {
        return avatar_url;
    }

    public void setAvatar_url(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public int getStatus() {
        return statusValue;
    }

    public void setStatus(int status) {
        this.statusValue = status;
    }

    public boolean isShowContact() {
        return showContact;
    }

    public void setShowContact(boolean showContact) {
        this.showContact = showContact;
    }

    public String getDisplayName() {
        //check fullname is empty or only have space char
        if (fullname != null && !fullname.isEmpty() && !fullname.trim().isEmpty()) {
            return fullname;
        } else if(username != null && !username.isEmpty()){
            return username;
        } else {
            return DEFAULT_DISPLAY_NAME_PREFIX + id;
        }
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }
}