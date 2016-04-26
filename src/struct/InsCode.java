package struct;

import java.util.List;

public class InsCode {
	private String returnType;
	private List<String> params;
	private int stack;
	private int locals;
	private int args_size;
	private String code;

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	public List<String> getParams() {
		return params;
	}

	public void setParams(List<String> params) {
		this.params = params;
	}

	public int getStack() {
		return stack;
	}

	public void setStack(int stack) {
		this.stack = stack;
	}

	public int getLocals() {
		return locals;
	}

	public void setLocals(int locals) {
		this.locals = locals;
	}

	public int getArgs_size() {
		return args_size;
	}

	public void setArgs_size(int args_size) {
		this.args_size = args_size;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
