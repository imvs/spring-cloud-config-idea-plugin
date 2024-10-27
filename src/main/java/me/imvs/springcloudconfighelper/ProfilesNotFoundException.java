package me.imvs.springcloudconfighelper;


import lombok.Getter;

import java.util.Collection;

@Getter
public class ProfilesNotFoundException extends RuntimeException {
    private final Collection<String> profiles;

    public ProfilesNotFoundException(Collection<String> profiles) {
        this.profiles = profiles;
    }
}
