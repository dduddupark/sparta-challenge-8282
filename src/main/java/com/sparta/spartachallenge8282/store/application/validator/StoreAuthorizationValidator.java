package com.sparta.spartachallenge8282.store.application.validator;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import org.springframework.stereotype.Component;

@Component
public class StoreAuthorizationValidator {

    //권한 검증 ==============
    /**
     * 가게 등록 신청 권한 검사
     *
     * CUSTOMER와 OWNER 모두 신청 가능하다.
     */
    public void validateStoreApplicationRole(UserDetailsImpl userDetails) {
        boolean isCustomer = UserRole.CUSTOMER.getAuthority().equals(userDetails.role());
        boolean isOwner = UserRole.OWNER.getAuthority().equals(userDetails.role());
        if(!isCustomer && !isOwner){
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }



    /**
     * 관리자 승인·거절 권한 검사
     */
    public void validateManagerRole(UserDetailsImpl userDetails) {
        boolean isManager = UserRole.MANAGER.getAuthority().equals(userDetails.role());
        boolean isMaster = UserRole.MASTER.getAuthority().equals(userDetails.role());

        if(!isManager && !isMaster){
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

}
