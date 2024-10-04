package me.imvs.springcloudconfighelper;


import lombok.Getter;

@Getter
public class ProfileNotFoundException extends RuntimeException {
    private final String profile;

    public ProfileNotFoundException(String profile) {
        this.profile = profile;
    }

}
