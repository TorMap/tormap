# Frontend

The frontend is built with [ReactJS](https://reactjs.org/)
and [Material-UI](https://mui.com/) frameworks and written in [TypeScript](https://www.typescriptlang.org/). To
visualize markers on a world map we use [Leaflet](https://leafletjs.com/).

## Requirements

- [NodeJS](https://nodejs.org/en/)  (v20)
- [yarn](https://yarnpkg.com/en/docs/install)

## Run Development Server

1. Go into `frontend` directory
2. Run command: `yarn install`
3. Run command: `yarn start`
4. Frontend should be available at http://localhost:3000

Run `yarn build`, to create a production ready build in `build` folder.

If you want to test the frontend against a local backend, you can change the environment variable `VITE_BACKEND_API_URL`
in the `.env` file.

## Config

The main `frontend` config is located at `frontend/srv/util/config.ts`. Further environment options like enable/disable
Browser autostart and default port can be configured in `frontend/.env`. Dependencies are managed with `yarn` and
located in `frontend/package.json`. Compiler options for `TypeScript` are located at `frontend/tsconfig.json`.

## Deploy

First make sure you have installed all requirements for development.

1. Go to `frontend` directory where file `package.json` is located
2. Run command: `yarn build`
3. A `frontend/build` folder should be generated containing all necessary frontend files
4. Go into the directory where `index.html` is located
5. Install [serve](https://www.npmjs.com/package/serve) and run command: `serve -l 3000`
6. Frontend should be available at http://localhost:3000
