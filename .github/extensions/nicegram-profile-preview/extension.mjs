import { createCanvas, CanvasError } from '@github/copilot-sdk/extension';
import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const instances = new Map();

const canvas = createCanvas({
  id: 'nicegram-profile-preview',
  description: 'Interactive preview of the NiceGram iOS-style profile',
  inputSchema: {
    type: 'object',
    properties: {}
  },
  actions: [
    {
      name: 'setTheme',
      description: 'Switch the preview between light and dark themes',
      inputSchema: {
        type: 'object',
        properties: {
          theme: { type: 'string', enum: ['light', 'dark'] }
        },
        required: ['theme'],
        additionalProperties: false
      },
      handler: async (_instanceId, input) => {
        const instance = instances.get(_instanceId);
        if (!instance) throw new CanvasError('NOT_FOUND', 'Preview instance not found');
        instance.theme = input.theme;
        return { theme: instance.theme };
      }
    }
  ],
  open: async ({ instanceId }) => {
    let instance = instances.get(instanceId);
    if (instance) return instance.url;

    const html = await readFile(join(here, 'profile.html'), 'utf8');
    const server = http.createServer(async (request, response) => {
      if (request.url === '/health') {
        response.writeHead(200, { 'content-type': 'text/plain' });
        response.end('ok');
        return;
      }
      if (request.url === '/state') {
        const instance = instances.get(instanceId);
        response.writeHead(200, {
          'content-type': 'application/json',
          'cache-control': 'no-store'
        });
        response.end(JSON.stringify({ theme: instance?.theme || 'light' }));
        return;
      }
      response.writeHead(200, {
        'content-type': 'text/html; charset=utf-8',
        'cache-control': 'no-store'
      });
      response.end(html);
    });

    await new Promise((resolve, reject) => {
      server.once('error', reject);
      server.listen(0, '127.0.0.1', resolve);
    });
    const address = server.address();
    if (!address || typeof address === 'string') {
      throw new CanvasError('SERVER_ERROR', 'Could not start preview server');
    }

    instance = { server, url: `http://127.0.0.1:${address.port}/`, theme: 'light' };
    instances.set(instanceId, instance);
    return instance.url;
  },
  onClose: async ({ instanceId }) => {
    const instance = instances.get(instanceId);
    if (instance) {
      await new Promise(resolve => instance.server.close(resolve));
      instances.delete(instanceId);
    }
  }
});

export default canvas;
