package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 从http报文中读取header的结果
 *
 * @author z:45:43
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AR {

	private ZArray array;
	
	private int headerEndIndex;

}
