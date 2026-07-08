#!/usr/bin/env python3
"""Small Flask-like server using Python's http.server and a minimal API.
Provides endpoints:
 - GET /api/examples -> JSON list of example names
 - POST /api/run {example: 'toffoli'} -> runs java CLI to produce JSON and returns it
Serves static files from this directory.
"""
import json, subprocess, os, sys, urllib.parse
from http.server import SimpleHTTPRequestHandler, HTTPServer

SITE_DIR = os.path.dirname(__file__)
JAVA_JAR = os.path.abspath(os.path.join(SITE_DIR, '..', 'target', 'javaqc.jar'))
EXAMPLES = ['bell','ghz','swap','toffoli']
PROVIDERS = ['local','ibm','google','azure','xanadu','qiskit','dwave','qmware','pasqual']


def describe_circuit(program):
    steps = program.get('steps', []) if isinstance(program, dict) else []
    lines = []
    for index, step in enumerate(steps, start=1):
        gates = step.get('gates', []) if isinstance(step, dict) else []
        gate_bits = []
        for gate in gates:
            if not isinstance(gate, dict):
                continue
            caption = gate.get('caption') or gate.get('type') or 'Gate'
            targets = gate.get('targets', [])
            gate_bits.append(f"{caption}{targets}")
        lines.append(f"Step {index}: " + (", ".join(gate_bits) if gate_bits else "<empty>"))
    return lines

class Handler(SimpleHTTPRequestHandler):
    def translate_path(self, path):
        # serve files from site directory
        rel = path.split('?',1)[0].lstrip('/')
        full = os.path.join(SITE_DIR, rel)
        return full

    def do_GET(self):
        if self.path.startswith('/api/examples'):
            self.send_response(200)
            self.send_header('Content-Type','application/json')
            self.end_headers()
            self.wfile.write(json.dumps(EXAMPLES).encode())
            return
        if self.path.startswith('/api/providers'):
            self.send_response(200)
            self.send_header('Content-Type','application/json')
            self.end_headers()
            self.wfile.write(json.dumps(PROVIDERS).encode())
            return
        return super().do_GET()

    def do_POST(self):
        if self.path.startswith('/api/run'):
            length = int(self.headers.get('content-length',0))
            body = self.rfile.read(length).decode('utf-8')
            data = json.loads(body) if body else {}
            ex = data.get('example','bell')
            provider = data.get('provider','local')
            program = data.get('program')
            pasqal_email = data.get('pasqalEmail')
            pasqal_project_id = data.get('pasqalProjectId')
            
            # Validate provider
            if provider not in PROVIDERS:
                provider = 'local'
            
            # invoke java CLI to write program.json to site/program.json
            outpath = os.path.join(SITE_DIR,'program.json')
            if program is not None:
                with open(outpath, 'w') as f:
                    json.dump(program, f, indent=2)
                print(f"[Pasqal UI] provider={provider} example={ex} email={pasqal_email or os.getenv('PASQAL_EMAIL','')} project={pasqal_project_id or os.getenv('PASQAL_PROJECT_ID','')}")
                for line in describe_circuit(program):
                    print(f"[Pasqal UI] {line}")
                self.send_response(200)
                self.send_header('Content-Type','application/json')
                self.end_headers()
                self.wfile.write(json.dumps(program).encode())
                return

            cmd = ['java','-jar',JAVA_JAR,'--export-json',outpath,'--example',ex,'--provider',provider]
            try:
                env = os.environ.copy()
                if pasqal_email:
                    env['PASQAL_EMAIL'] = pasqal_email
                if pasqal_project_id:
                    env['PASQAL_PROJECT_ID'] = pasqal_project_id
                p = subprocess.run(cmd, cwd=os.path.join(SITE_DIR,'..'), env=env, capture_output=True, text=True, timeout=20)
                if p.stdout:
                    print(p.stdout, end='')
                if p.stderr:
                    print(p.stderr, end='', file=sys.stderr)
                if p.returncode != 0:
                    self.send_response(500)
                    self.send_header('Content-Type','application/json')
                    self.end_headers()
                    self.wfile.write(json.dumps({'error':'java failed','stdout':p.stdout,'stderr':p.stderr}).encode())
                    return
                # return the JSON content
                with open(outpath,'r') as f:
                    prog = json.load(f)
                self.send_response(200)
                self.send_header('Content-Type','application/json')
                self.end_headers()
                self.wfile.write(json.dumps(prog).encode())
                return
            except Exception as exn:
                self.send_response(500)
                self.send_header('Content-Type','application/json')
                self.end_headers()
                self.wfile.write(json.dumps({'error':str(exn)}).encode())
                return
        return super().do_POST()

if __name__=='__main__':
    port = int(os.environ.get('PORT','8000'))
    os.chdir(SITE_DIR)
    server = HTTPServer(('0.0.0.0',port), Handler)
    print('Serving site on http://0.0.0.0:%d' % port)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print('stopping')
        server.server_close()
