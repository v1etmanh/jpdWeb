 package com.jpd.web.exception;

public class WishlistExistException extends BusinessException{

	public WishlistExistException(String message) {
        super("WISHLIST_ALREADY_EXISTS", message,
              "Khóa học này đã có trong danh sách yêu thích");
    }

}
