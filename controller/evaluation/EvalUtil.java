package controller.evaluation;

import controller.exestack.ExeStack;
import controller.exestack.MyDeque;
import controller.symtable.SymTable;
import exceptions.DivisionByZero;
import exceptions.StmtException;
import exceptions.SymbolException;
import exceptions.TypeException;
import model.statements.*;
import model.symbol.*;

public class EvalUtil {

    public static ISymbol lookUp(SymTable<String, ISymbol> table, String label) {
        // Null if it can't find one or the sym.
        return table.getSymbol(label);
    }

    // Evaluating the Integer symbols
    public static boolean isInt(ISymbol sym) {
        if (sym.getType() == "Int") {
            return true;
        }
        return false;
    }

    public static Integer evalInt(ISymbol sym) {
        if (EvalUtil.isInt(sym)) {
            return ((SymInteger) sym).getValue();
        }
        return null;
    }

    // Evaluating the Boolean symbols
    public static boolean isBool(ISymbol sym) {
        if (sym.getType() == "Bool") {
            return true;
        }
        return false;
    }

    public static Boolean evalBoolean(ISymbol sym) {
        if (EvalUtil.isBool(sym)) {
            return ((SymBoolean) sym).getValue();
        }
        return null;
    }

    // Is numeric and Is boolean functions
    public static Integer isNumeric(String strNum) {
        Integer num;
        if (strNum == null) {
            return null;
        }
        try {
            num = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return null;
        }
        return num;
    }

    public static Boolean isBoolean(String strBool) {
        if (strBool == null) {
            return null;
        }
        Boolean b;
        try {
            b = Boolean.parseBoolean(strBool);
        } catch (NumberFormatException nfe) {
            return null;
        }
        return b;
    }

    public static boolean isIfStmt(IStmt statement) {
        if (((IfStmt) statement).getContents().startsWith("If")) {
            return true;
        }
        return false;

    }

    public static boolean isAssignStmt(IStmt statement) {
        String tok = ((AssignStmt) statement).getWords()[1];
        if (tok.startsWith("=")) {
            return true;
        }
        return false;
    }

    public static boolean isPrintStmt(IStmt statement) {
        String first = ((PrintStmt) statement).getContents();
        if (first.startsWith("Print")) {
            return true;
        }
        return false;

    }

    public static boolean isVarDecl(IStmt statement) {
        if (((VarDecl) statement).getWords()[0].startsWith("Int")
                || ((VarDecl) statement).getWords()[0].startsWith("Bool")) {
            return true;

        }
        return false;
    }

    // They eval expressions, no matter the terms
    public static Integer evalArithemtic(ISymbol sym1, ISymbol sym2, String operator)
            throws DivisionByZero, TypeException {
        if (((SymInteger) sym1).getType() == "Int") {
            if (((SymInteger) sym2).getType() == "Int") {
                Integer nr1 = ((SymInteger) sym1).getValue();
                Integer nr2 = ((SymInteger) sym2).getValue();
                if (operator.startsWith("+")) {
                    return nr1 + nr2;
                }
                if (operator.startsWith("*")) {
                    return nr1 * nr2;
                }
                if (operator.startsWith("-")) {
                    return nr1 - nr2;
                }
                if (operator.startsWith("/")) {
                    if (nr2 != 0) {
                        return nr1 / nr2;
                    }
                    throw new DivisionByZero("Cannot divide by zero!");
                }
            }
            throw new TypeException("Operands are not of same type");
        }
        throw new TypeException("Operands are not of same type");
    }

    public static Boolean evalLogical(ISymbol sym1, ISymbol sym2, String operator) throws TypeException {
        if (sym1.getType() == "Bool") {
            if (sym2.getType() == "Bool") {
                Boolean s1 = ((SymBoolean) sym1).getValue();
                Boolean s2 = ((SymBoolean) sym2).getValue();
                if (operator.startsWith("and")) {
                    return s1 && s2;
                }
                if (operator.startsWith("or")) {
                    return s1 || s2;
                }
            }
            throw new TypeException("Operands are not of same type");
        }
        throw new TypeException("Operands are not of same type");

    }

    /**
     * @param table
     * @param stmt
     * @return returns the statement if it is of the type it's processing, null
     *         otherwise
     * @throws TypeException
     * @throws SymbolException
     * @throws DivisionByZero
     */
    public static AssignStmt processAssign(SymTable<String, ISymbol> table, IStmt stmt)
            throws TypeException, SymbolException, DivisionByZero {
        if (EvalUtil.isAssignStmt(stmt)) { // First we check if it's an assign statement
            try {
                String[] exp = ((AssignStmt) stmt).getWords();
                ISymbol sym = EvalUtil.lookUp(table, exp[0]); // We get the symbol, lookUp automatically checks if it's in
                                                          // the table
                if (sym == null) {
                    throw new SymbolException("Variable is not declared.");
                }
                if (EvalUtil.isInt(sym)) { // If our symbol is an integer
                    Integer rez = 0;
                    if (exp.length == 3) { // If it's just a simple assignment
                        rez = EvalUtil.isNumeric(exp[2]);
                        if (rez != null) { // Checking if we are assigning a variable only a value
                            table.setSymbol(sym.getLabel(), new SymInteger(rez, sym.getLabel()));
                            return (AssignStmt) stmt;
                        } else { // If it's not a value, than it is the value of another variable
                            rez = ((SymInteger) EvalUtil.lookUp(table, exp[2])).getValue();
                            if (rez == null) {
                                throw new SymbolException("Variable is not declared.");
                            }
                            table.setSymbol(sym.getLabel(), new SymInteger(rez, sym.getLabel()));
                            return (AssignStmt) stmt;
                        }
                    } else { // If it's a compund assingment
                        // First value of the assignment
                        rez = EvalUtil.isNumeric(exp[2]);
                        if (rez == null)
                            rez = ((SymInteger) EvalUtil.lookUp(table, exp[2])).getValue();
                        if (rez == null) {
                            throw new SymbolException("Variable is not declared.");
                        }
                        // Rest of the values
                        for (int i = 3; i < exp.length - 1; i = i + 2) {
                            ISymbol s2 = EvalUtil.lookUp(table, exp[i + 1]);
                            if (s2 != null) {
                                rez += EvalUtil.evalArithemtic(new SymInteger(0, ""), s2, exp[i]);
                            } else {
                                Integer perm = EvalUtil.isNumeric(exp[i + 1]);
                                if (perm == null) {
                                    throw new SymbolException("Variable is not declared.");
                                }
                                Integer aux = EvalUtil.evalArithemtic(new SymInteger(0, ""),
                                        new SymInteger(perm, ""), exp[i]);
                                rez += aux;
                            }
                        }
                        table.setSymbol(sym.getLabel(), new SymInteger(rez, sym.getLabel()));
                        return (AssignStmt) stmt;

                    }
                }
                // Analog for the boolean value
                if (EvalUtil.isBool(sym)) {
                    Boolean rez = null;
                    if (exp.length == 3) {
                        rez = EvalUtil.isBoolean(exp[2]);
                        if (rez != null) {
                            table.setSymbol(sym.getLabel(), new SymBoolean(rez, sym.getLabel()));
                            return (AssignStmt) stmt;
                        } else {
                            rez = ((SymBoolean) EvalUtil.lookUp(table, exp[2])).getValue();
                            if (rez == null) {
                                throw new SymbolException("Variable is not declared.");
                            }
                            table.setSymbol(sym.getLabel(), new SymBoolean(rez, sym.getLabel()));
                            return (AssignStmt) stmt;
                        }
                    } else {
                        rez = EvalUtil.isBoolean(exp[2]);
                        if (rez == null)
                            rez = ((SymBoolean) EvalUtil.lookUp(table, exp[2])).getValue();
                        if (rez == null) {
                            throw new SymbolException("Variable is not declared.");
                        }
                        for (int i = 3; i < exp.length - 1; i = i + 2) {
                            ISymbol s2 = EvalUtil.lookUp(table, exp[i + 1]);
                            if (s2 != null) {
                                rez = EvalUtil.evalLogical(new SymBoolean(rez, ""), s2, exp[i]);
                            } else {
                                Boolean perm = EvalUtil.isBoolean(exp[i + 1]);
                                if (perm == null) {
                                    throw new SymbolException("Variable is not declared.");
                                }
                                rez = EvalUtil.evalLogical(new SymBoolean(rez, ""),
                                        new SymBoolean(perm, ""), exp[i]);
                            }
                        }
                        table.setSymbol(sym.getLabel(), new SymBoolean(rez, sym.getLabel()));
                        return (AssignStmt) stmt;
                    }
                }
            } catch (SymbolException e) {
                throw new SymbolException(e.getMessage());
            } catch (DivisionByZero d) {
                throw new DivisionByZero(d.getMessage());
            } catch (TypeException t) {
                throw new TypeException(t.getMessage());
            }
        }
        return (AssignStmt) null;
    }

    public static VarDecl processDecl(SymTable<String, ISymbol> table, IStmt v) throws SymbolException {
        String[] exp = ((VarDecl) v).getWords();
        if (EvalUtil.isVarDecl(v)) {
            if (exp[0].startsWith("Int")) {
                SymInteger s = new SymInteger(exp[1]);
                if (EvalUtil.lookUp(table, exp[1]) == null) {
                    table.addSymbol(s.getLabel(), s);
                    return (VarDecl) v;
                }
                throw new SymbolException("Variable: " + s.getLabel() + " cannot be added twice.");
            }
            if (exp[0].startsWith("Bool")) {
                SymBoolean s = new SymBoolean(exp[1]);
                if (EvalUtil.lookUp(table, exp[1]) == null) {
                    table.addSymbol(s.getLabel(), s);
                    return (VarDecl) v;
                }
                throw new SymbolException("Variable: " + s.getLabel() + " cannot be added twice.");
            }
            throw new SymbolException("Syntax error!");
        }
        return (VarDecl) null;
    }

    public static PrintStmt processPrint(MyDeque<String> output, SymTable<String, ISymbol> table, IStmt v)
            throws SymbolException {
        if (EvalUtil.isPrintStmt(v)) {
            try {
                String[] exp = ((PrintStmt) v).getWords();
                if (exp[1].startsWith("\"")) {
                    String rez = ((PrintStmt) v).getWords()[1].split("\"")[1];
                    output.addFirst(rez);
                    return (PrintStmt) v;
                }
                String label = exp[1].split("\\)")[0];
                ISymbol sym = EvalUtil.lookUp(table, label);
                if (sym == null) {
                    throw new SymbolException("Variable is not declared.");
                }
                if (sym.getType() == "Int") {
                    output.addFirst(Integer.toString(((SymInteger) sym).getValue()));
                    return (PrintStmt) v;
                }
                if (sym.getType() == "Bool") {
                    output.addFirst(Boolean.toString(((SymBoolean) sym).getValue()));
                    return (PrintStmt) v;
                }

            } catch (SymbolException s) {
                throw new SymbolException(s.getMessage());
            }
        }
        return (PrintStmt) null;

    }

    public static IfStmt processConditional(ExeStack stack, MyDeque<String> output, SymTable<String, ISymbol> table,
            IStmt conditional) throws SymbolException, TypeException, DivisionByZero, StmtException {
        if (EvalUtil.isIfStmt(conditional)) {
            try {
                String[] exp = ((IfStmt) conditional).getWords(); // Splitting into expressions
                String[] firstExp = exp[1].split(" ");
                if (firstExp.length == 1) { // First case, it has only a variable
                    Integer a = EvalUtil.isNumeric(firstExp[0]);
                    Boolean b = EvalUtil.isBoolean(firstExp[0]);
                    ISymbol sym = EvalUtil.lookUp(table, firstExp[0]);
                    if (sym == null && a == null && b == null) {
                        throw new SymbolException("Condition cannot be evaluated");
                    }
                    if (sym.getType() == "Int") {
                        if (((SymInteger) sym).getValue() != 0 || (a != null && a != 0)) { // Evaluating it as true
                            // Can only be an assignment or a print statement
                            AssignStmt checkAssign = new AssignStmt(exp[3]);
                            checkAssign = EvalUtil.processAssign(table, checkAssign);
                            if (checkAssign == null) { // If it's not one, the it's the other
                                PrintStmt checkPrint = new PrintStmt(exp[3]);
                                checkPrint = EvalUtil.processPrint(output, table, checkPrint);
                                if (checkPrint == null) {
                                    throw new StmtException("Instruction cannot be done");
                                }
                                stack.addFirst(checkPrint);
                            } else
                                stack.addFirst(checkAssign);
                        }

                        if (exp.length == 4) {
                            return (IfStmt) conditional;
                        } else {
                            // Now we do the else branch
                            AssignStmt checkAssign = new AssignStmt(exp[5]);
                            checkAssign = EvalUtil.processAssign(table, checkAssign);
                            if (checkAssign == null) { // If it's not one, the it's the other
                                PrintStmt checkPrint = new PrintStmt(exp[5]);
                                checkPrint = EvalUtil.processPrint(output, table, checkPrint);
                                if (checkPrint == null) {
                                    throw new StmtException("Instruction cannot be done");
                                }
                                stack.addFirst(checkPrint);
                                return (IfStmt) conditional;
                            } else {
                                stack.addFirst(checkAssign);
                                return (IfStmt) conditional;
                            }
                        }

                    }

                    if (sym.getType() == "Bool") {
                        if (((SymBoolean) sym).getValue() != false || (b != null && b != false)) {
                            // Evaluating it as true.
                            // Can only be an assignment or a print statement
                            AssignStmt checkAssign = new AssignStmt(exp[3]);
                            checkAssign = EvalUtil.processAssign(table, checkAssign);
                            if (checkAssign == null) { // If it's not one, the it's the other
                                PrintStmt checkPrint = new PrintStmt(exp[3]);
                                checkPrint = EvalUtil.processPrint(output, table, checkPrint);
                                if (checkPrint == null) {
                                    throw new StmtException("Instruction cannot be done");
                                }
                                stack.addFirst(checkPrint);
                            } else
                                stack.addFirst(checkAssign);
                        }
                        if (exp.length == 4) {
                            return (IfStmt) conditional;
                        } else {
                            // Now we do the else branch
                            AssignStmt checkAssign = new AssignStmt(exp[5]);
                            checkAssign = EvalUtil.processAssign(table, checkAssign);
                            if (checkAssign == null) { // If it's not one, the it's the other
                                PrintStmt checkPrint = new PrintStmt(exp[5]);
                                checkPrint = EvalUtil.processPrint(output, table, checkPrint);
                                if (checkPrint == null) {
                                    throw new StmtException("Instruction cannot be done");
                                }
                                stack.addFirst(checkPrint);
                            } else {
                                stack.addFirst(checkAssign);
                                return (IfStmt) conditional;
                            }

                        }
                        return (IfStmt) null;
                    }
                }

            } catch (SymbolException e) {
                throw new SymbolException(e.getMessage());
            } catch (TypeException t) {
                throw new TypeException(t.getMessage());
            } catch (DivisionByZero d) {
                throw new DivisionByZero(d.getMessage());
            }
        }
        return (IfStmt) null;
    }
}