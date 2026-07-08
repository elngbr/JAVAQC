// public class localSimulator {
//     public static void main(String[] args) {
//         // Create a simple quantum program with 2 qubits
//         Program program = new Program(2);
//         program.addGate(new org.redfx.strange.gate.Hadamard(0));
//         program.addGate(new org.redfx.strange.gate.Cnot(0, 1));

//         // Create an instance of the local simulator
//         org.redfx.strange.local.LocalSimulator simulator = new org.redfx.strange.local.LocalSimulator();

//         try {
//             // Run the program on the local simulator
//             Result result = simulator.runProgram(program);

//             // Print the measurement results
//             System.out.println("Measurement results: " + result.getMeasurements());
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }
    
// }
