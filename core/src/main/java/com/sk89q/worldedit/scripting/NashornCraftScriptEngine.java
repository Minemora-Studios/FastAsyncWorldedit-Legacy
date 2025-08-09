/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.scripting;

import com.boydti.fawe.Fawe;
import com.sk89q.worldedit.WorldEditException;

import javax.script.*;
import java.util.Map;

public class NashornCraftScriptEngine implements CraftScriptEngine {

    // GraalJS a través de javax.script
    // js-scriptengine registra los nombres "graal.js" y "js".
    private static volatile ScriptEngineManager MANAGER;

    private int timeLimit;

    @Override
    public void setTimeLimit(int milliseconds) {
        // Nota: Limitar tiempo de ejecución no está soportado nativamente por javax.script + GraalJS CE.
        // Podrías ejecutar en otro hilo y cancelar/interrumpir si te interesa hacer enforcement.
        this.timeLimit = milliseconds;
    }

    @Override
    public int getTimeLimit() {
        return timeLimit;
    }

    @Override
    public Object evaluate(String script, String filename, Map<String, Object> args) throws Throwable {
        // Mantener el classloader de FAWE como contexto, igual que antes
        ClassLoader cl = Fawe.get().getClass().getClassLoader();
        Thread current = Thread.currentThread();
        ClassLoader previousCl = current.getContextClassLoader();
        current.setContextClassLoader(cl);

        try {
            ScriptEngine engine = getEngine();
            if (engine == null) {
                throw new IllegalStateException("No se encontró GraalJS en el classpath. " +
                        "Asegúrate de tener las dependencias 'org.graalvm.js:js' y 'org.graalvm.js:js-scriptengine'.");
            }

            // Pasar el "filename" (opcional): algunos motores lo usan para el nombre en los stacktraces.
            engine.put(ScriptEngine.FILENAME, filename != null ? filename : "<script>");

            // Bindings con los argumentos
            Bindings bindings = new SimpleBindings();
            if (args != null) {
                for (Map.Entry<String, Object> entry : args.entrySet()) {
                    bindings.put(entry.getKey(), entry.getValue());
                }
            }

            try {
                return engine.eval(script, bindings);
            } catch (Error e) {
                e.printStackTrace();
                throw new ScriptException(e.getMessage());
            } catch (Throwable e) {
                e.printStackTrace();
                while (e.getCause() != null) {
                    e = e.getCause();
                }
                if (e instanceof WorldEditException) {
                    throw e;
                }
                throw e;
            }
        } finally {
            // Restaurar el classloader previo
            current.setContextClassLoader(previousCl);
        }
    }

    private static ScriptEngine getEngine() {
        ScriptEngineManager manager = MANAGER;
        if (manager == null) {
            synchronized (NashornCraftScriptEngine.class) {
                manager = MANAGER;
                if (manager == null) {
                    MANAGER = manager = new ScriptEngineManager();
                }
            }
        }

        // Intentar primero "graal.js" y luego "js"
        ScriptEngine engine = manager.getEngineByName("graal.js");
        if (engine == null) {
            engine = manager.getEngineByName("js");
        }

        // Si quieres activar opciones de GraalJS vía propiedades del sistema, puedes hacerlo
        // ANTES de crear el engine (por ejemplo en el arranque del plugin):
        // System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        // System.setProperty("js.ecmascript-version", "2022"); // ejemplo

        return engine;
    }
}