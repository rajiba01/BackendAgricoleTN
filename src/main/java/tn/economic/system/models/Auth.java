package tn.economic.system.models;




public class Auth {
    private String email;
    private String mdp;

    public Auth() {
    }

    public Auth(String email , String mdp){
        this.mdp=mdp;
        this.email=email;
    }

  public  String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email=email;
    }
    public void setMdp(String mdp){
        this.mdp=mdp;
    }
    public String getMdp(){
        return mdp;
    }

}
