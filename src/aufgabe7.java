import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.time.LocalDate;

public class Aufgabe7 extends JFrame {
    static Connection conn;
    static Statement stmt;
    JComboBox<String> land;
    JComboBox<String> ausstattung;
    JTextField anreisedatum;
    JTextField abreisedatum;

    public Aufgabe7() {
        this.setTitle("Ferienwohnungen Suchen");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String[] laender = {"Deutschland", "Frankreich", "Italien", "Spanien", "Schweiz", "Österreich"};
        String[] ausstattungen = {"-", "Balkon", "Sauna", "TV", "WLAN", "Küche"};


        JLabel landLabel = new JLabel("Land:");
        land = new JComboBox<>(laender);
        JLabel ausstattungLabel = new JLabel("Ausstattung:");
        ausstattung = new JComboBox<>(ausstattungen);

        JLabel anreiseLabel = new JLabel("Anreise Datum:");
        JLabel abreiseLabel = new JLabel("Abreise Datum:");
        anreisedatum = new JTextField(LocalDate.now().toString(), 10);
        abreisedatum = new JTextField(LocalDate.now().plusDays(3).toString(), 10);

        JButton suchButton = new JButton("Suchen");
        suchButton.addActionListener(e -> {
            try {
                StringBuilder sql = new StringBuilder("SELECT f.Ferienwohnungsname, AVG(b.Sterne) AS Durchschnittsbewertung " +
                        "FROM dbsys43.Ferienwohnung f " +
                        "JOIN dbsys43.Adresse a  ON f.AdresseID = a.AdresseID " +
                        "JOIN dbsys43.Land l     ON a.LandName = l.LandName " +
                        "JOIN dbsys43.Besitzt be ON f.Ferienwohnungsname = be.Ferienwohnungsname " +
                        "LEFT JOIN dbsys43.Buchung b ON f.Ferienwohnungsname = b.Ferienwohnungsname " +
                        "WHERE l.LandName = ? ");      // 1 — Land
                if (ausstattung.getSelectedItem().toString() != "-") {
                    sql.append("AND   be.Ausstattungsname = ? ");
                }
                sql.append("AND   f.Ferienwohnungsname NOT IN ( " +
                        "        SELECT f2.Ferienwohnungsname " +
                        "        FROM dbsys43.Buchung b2 " +
                        "        JOIN dbsys43.Ferienwohnung f2 ON b2.Ferienwohnungsname = f2.Ferienwohnungsname " +
                        "        WHERE ( b2.Startdatum BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD') " +   // 3,4 — Start- & Enddatum
                        "                OR b2.Enddatum   BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD') " +// 5,6 — Start- & Enddatum
                        "                OR ( b2.Startdatum <= TO_DATE(?, 'YYYY-MM-DD') AND b2.Enddatum >= TO_DATE(?, 'YYYY-MM-DD') ) ) " + // 7,8
                        "      ) " +
                        "GROUP BY f.Ferienwohnungsname " +
                        "ORDER BY AVG(b.Sterne) DESC NULLS LAST");
                PreparedStatement ps = conn.prepareStatement(String.valueOf(sql));

                int counter = 1;
                ps.setString(counter, land.getSelectedItem().toString());
                counter++;
                if (ausstattung.getSelectedItem().toString() != "-") {
                    ps.setString(counter, ausstattung.getSelectedItem().toString());
                    counter++;
                }
                ps.setDate(counter, Date.valueOf(anreisedatum.getText()));
                counter++;
                ps.setDate(counter, Date.valueOf(abreisedatum.getText()));
                counter++;
                ps.setDate(counter, Date.valueOf(anreisedatum.getText()));
                counter++;
                ps.setDate(counter, Date.valueOf(abreisedatum.getText()));
                counter++;
                ps.setDate(counter, Date.valueOf(anreisedatum.getText()));
                counter++;
                ps.setDate(counter, Date.valueOf(abreisedatum.getText()));
                counter++;

                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String r1 = rs.getString("ferienwohnungsname");
                    double schnitt = rs.getDouble("Durchschnittsbewertung");
                    System.out.printf("%-30s  %.2f Sterne%n", r1, schnitt);
                }

                rs.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    stmt.close();
                    conn.close();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }

            }
        });

        JPanel jpanel = new JPanel();
        jpanel.setLayout(new GridLayout(5, 2));
        jpanel.add(landLabel);
        jpanel.add(land);
        jpanel.add(ausstattungLabel);
        jpanel.add(ausstattung);
        jpanel.add(anreiseLabel);
        jpanel.add(anreisedatum);
        jpanel.add(abreiseLabel);
        jpanel.add(abreisedatum);
        jpanel.add(suchButton);
        jpanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(jpanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        JFrame myAppl = new Aufgabe7();

        String url = "jdbc:oracle:thin:@oracle19c.in.htwg-konstanz.de:1521:ora19c";
        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            conn = DriverManager.getConnection(url, "dbsys64", "dbsys64");
            stmt = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
