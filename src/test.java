import javax.swing.*;
import java.sql.*;

public class test {
    public static void main(String[] args) {
        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            String url= "jdbc:oracle:thin:@oracle19c.in.htwg-konstanz.de:1521:ora19c";
            Connection conn = null;
            conn = DriverManager.getConnection(url, "dbsys43", "daniel2603");
            Statement stmt = conn.createStatement();

            String sql =
                    "SELECT f.Ferienwohnungsname, AVG(b.Sterne) AS Durchschnittsbewertung " +
                            "FROM dbsys43.Ferienwohnung f " +
                            "JOIN dbsys43.Adresse a  ON f.AdresseID = a.AdresseID " +
                            "JOIN dbsys43.Land l     ON a.LandName = l.LandName " +
                            "JOIN dbsys43.Besitzt be ON f.Ferienwohnungsname = be.Ferienwohnungsname " +
                            "LEFT JOIN dbsys43.Buchung b ON f.Ferienwohnungsname = b.Ferienwohnungsname " +
                            "WHERE l.LandName = ? " +                             // 1 — Land
                            "AND   be.Ausstattungsname = ? " +                    // 2 — Ausstattung
                            "AND   f.Ferienwohnungsname NOT IN ( " +
                            "        SELECT f2.Ferienwohnungsname " +
                            "        FROM dbsys43.Buchung b2 " +
                            "        JOIN dbsys43.Ferienwohnung f2 ON b2.Ferienwohnungsname = f2.Ferienwohnungsname " +
                            "        WHERE ( b2.Startdatum BETWEEN ? AND ? " +   // 3,4 — Start- & Enddatum (BETWEEN 1)
                            "                OR b2.Enddatum   BETWEEN ? AND ? " +// 5,6 — Start- & Enddatum (BETWEEN 2)
                            "                OR ( b2.Startdatum <= ? AND b2.Enddatum >= ? ) ) " + // 7,8 — Start ≤, End ≥
                            "      ) " +
                            "GROUP BY f.Ferienwohnungsname " +
                            "ORDER BY AVG(b.Sterne) DESC NULLS LAST";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "Land");
            ps.setString(2, "Ausstattung");
            ps.setString(3, "Startdatum");
            ps.setString(4, "Enddatum");
            ps.setString(5, "Startdatum");
            ps.setString(6, "Enddatum");
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()){
                String r1 = rs.getString("ferienwohnungsname");
                System.out.println(r1);
            }

            rs.close(); stmt.close(); conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
