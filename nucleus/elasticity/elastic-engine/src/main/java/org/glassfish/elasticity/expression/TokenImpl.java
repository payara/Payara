package org.glassfish.elasticity.expression;

public class TokenImpl
	implements Token {
	
	private TokenType id;
	private String value;
	
	
	public TokenImpl(TokenType id, String value) {
		super();
		this.id = id;
		this.value = value;
	}
	
	public TokenType getTokenType() {
		return id; 
	}
	
	public String value() {
		return value;
	}		
	
	public String toString() {
		return value;
	}
}
