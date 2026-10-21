"use client";
import { FormEvent, useEffect, useState } from "react";
const A = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
type S = { user: { id: string; nombre: string }; token: string };
type T = { id: string; nombre: string; pokemon: any[] };
async function api<T>(p: string, o?: RequestInit): Promise<T> {
  const r = await fetch(A + p, {
      ...o,
      headers: { "Content-Type": "application/json", ...(o?.headers ?? {}) },
    }),
    d = await r.json().catch(() => ({}));
  if (!r.ok) throw Error(d.mensaje ?? `Error ${r.status}`);
  return d;
}
export default function Home() {
  const [s, setS] = useState<S | null>(null),
    [tab, setTab] = useState("teams"),
    [teams, setTeams] = useState<T[]>([]),
    [team, setTeam] = useState<T | null>(null),
    [note, setNote] = useState("Inicia sesión."),
    [q, setQ] = useState(""),
    [found, setFound] = useState<any[]>([]),
    [pick, setPick] = useState<any>(null),
    [moves, setMoves] = useState<any[]>([]),
    [ids, setIds] = useState<number[]>([]),
    [matchTeam, setMatchTeam] = useState(""),
    [code, setCode] = useState(""),
    [room, setRoom] = useState<any>(null);
  const run = async (f: () => Promise<void>) => {
    try {
      await f();
    } catch (e) {
      setNote(e instanceof Error ? e.message : "Error");
    }
  };
  const load = async (x = s) => {
    if (x) setTeams(await api(`/api/jugadores/${x.user.id}/equipos`));
  };
  useEffect(() => {
    const x = localStorage.getItem("pokebattle-session-v3");
    if (x) {
      const v = JSON.parse(x);
      setS(v);
      load(v);
    }
  }, []);
  const auth = async (e: FormEvent<HTMLFormElement>, p: string) => {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    await run(async () => {
      const r = await api<any>(p, {
          method: "POST",
          body: JSON.stringify({ usuario: f.get("u"), password: f.get("p") }),
        }),
        x = { user: r.jugador, token: r.accessToken };
      localStorage.setItem("pokebattle-session-v3", JSON.stringify(x));
      setS(x);
      await load(x);
      setNote(`Bienvenido, ${x.user.nombre}`);
    });
  };
  const select = async (id: string) =>
    await run(async () => setTeam(await api(`/api/equipos/${id}`)));
  const addTeam = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await run(async () => {
      if (!s) return;
      const n = new FormData(e.currentTarget).get("n"),
        t = await api<T>(`/api/jugadores/${s.user.id}/equipos`, {
          method: "POST",
          headers: { Authorization: `Bearer ${s.token}` },
          body: JSON.stringify({ nombre: n }),
        });
      setTeam(t);
      await load();
    });
  };
  const search = async (e: FormEvent) => {
    e.preventDefault();
    await run(async () =>
      setFound(await api(`/api/catalogo/pokemon/buscar?nombre=${q}`)),
    );
  };
  const choose = async (p: any) =>
    await run(async () => {
      setPick(p);
      setIds([]);
      setMoves(
        (
          await api<any>(
            `/api/catalogo/pokemon/${p.id}/movimientos?page=0&size=50`,
          )
        ).contenido,
      );
    });
  const addPokemon = async () =>
    await run(async () => {
      if (!team || !pick || !ids.length) throw Error("Selecciona movimientos");
      await api(`/api/equipos/${team.id}/pokemon`, {
        method: "POST",
        body: JSON.stringify({ pokemonApiId: pick.id, movimientosApiId: ids }),
      });
      await select(team.id);
      await load();
      setPick(null);
    });
  const create = async () =>
    await run(async () => {
      if (!s || !matchTeam) throw Error("Selecciona un equipo");
      const r = await api<any>("/api/partidas", {
        method: "POST",
        body: JSON.stringify({ jugadorId: s.user.id, equipoId: matchTeam }),
      });
      setRoom(r);
      setNote(`Código de sala: ${r.codigoSala}`);
    });
  const join = async () =>
    await run(async () => {
      if (!s || !matchTeam || !code)
        throw Error("Selecciona equipo e ingresa código");
      const p = await api<any>(`/api/partidas/codigo/${code}`);
      setRoom(
        await api(`/api/partidas/${p.id}/unirse`, {
          method: "POST",
          body: JSON.stringify({ jugadorId: s.user.id, equipoId: matchTeam }),
        }),
      );
    });
  const logout = () => {
    localStorage.clear();
    sessionStorage.clear();
    document.cookie
      .split(";")
      .forEach(
        (c) =>
          (document.cookie = `${c.split("=")[0]}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`),
      );
    location.href = "/";
  };
  if (!s)
    return (
      <main>
        <header>
          <b>POKÉBATTLE</b>
          <h1>Tu aventura comienza</h1>
          <p>{note}</p>
        </header>
        <section className="panel auth">
          <form onSubmit={(e) => auth(e, "/api/auth/registro")}>
            <h2>Registro</h2>
            <input name="u" placeholder="Usuario" minLength={4} required />
            <input
              name="p"
              type="password"
              placeholder="Contraseña"
              minLength={8}
              required
            />
            <button>Crear cuenta</button>
          </form>
          <form onSubmit={(e) => auth(e, "/api/auth/login")}>
            <h2>Login</h2>
            <input name="u" placeholder="Usuario" required />
            <input name="p" type="password" placeholder="Contraseña" required />
            <button>Entrar</button>
          </form>
        </section>
      </main>
    );
  return (
    <main>
      <header>
        <b>POKÉBATTLE</b>
        <h1>Entrenador {s.user.nombre}</h1>
        <p>{note}</p>
        <nav>
          <button
            className={tab === "teams" ? "active" : ""}
            onClick={() => setTab("teams")}
          >
            Equipos
          </button>
          <button
            className={tab === "matches" ? "active" : ""}
            onClick={() => setTab("matches")}
          >
            Partidas
          </button>
          <button className="danger" onClick={logout}>
            Cerrar sesión
          </button>
        </nav>
      </header>
      {tab === "teams" ? (
        <section className="panel">
          <h2>Mis equipos</h2>
          <div className="choices">
            {teams.map((t) => (
              <button
                className={team?.id === t.id ? "active" : ""}
                onClick={() => select(t.id)}
                key={t.id}
              >
                {t.nombre} · {t.pokemon.length}/3
              </button>
            ))}
          </div>
          <form onSubmit={addTeam}>
            <input name="n" placeholder="Nuevo equipo" required />
            <button>Crear equipo</button>
          </form>
          {team && (
            <>
              <h2>Editar {team.nombre}</h2>
              <div className="roster">
                {team.pokemon.map((p: any) => (
                  <article key={p.id}>
                    <img src={p.sprite} alt="" />
                    <b>{p.nombre}</b>
                    <small>
                      {p.movimientos.map((m: any) => m.nombre).join(" · ")}
                    </small>
                  </article>
                ))}
              </div>
              {team.pokemon.length < 3 && (
                <>
                  <form onSubmit={search}>
                    <input
                      value={q}
                      onChange={(e) => setQ(e.target.value)}
                      placeholder="Buscar Pokémon"
                    />
                    <button>Buscar</button>
                  </form>
                  <div className="choices">
                    {found.map((p) => (
                      <button onClick={() => choose(p)} key={p.id}>
                        {p.nombre}
                      </button>
                    ))}
                  </div>
                </>
              )}
              {pick && (
                <div className="editor">
                  <h3>
                    {pick.nombre}: movimientos ({ids.length}/4)
                  </h3>
                  {moves.map((m) => (
                    <label key={m.id}>
                      <input
                        type="checkbox"
                        checked={ids.includes(m.id)}
                        onChange={() =>
                          setIds((x) =>
                            x.includes(m.id)
                              ? x.filter((i) => i !== m.id)
                              : x.length < 4
                                ? [...x, m.id]
                                : x,
                          )
                        }
                      />
                      {m.nombre}{" "}
                    </label>
                  ))}
                  <button onClick={addPokemon}>Agregar Pokémon</button>
                </div>
              )}
            </>
          )}
        </section>
      ) : (
        <section className="panel">
          <h2>Partidas</h2>
          <select
            value={matchTeam}
            onChange={(e) => setMatchTeam(e.target.value)}
          >
            <option value="">Selecciona equipo</option>
            {teams
              .filter((t) => t.pokemon.length === 3)
              .map((t) => (
                <option value={t.id} key={t.id}>
                  {t.nombre}
                </option>
              ))}
          </select>
          <button onClick={create}>Crear partida</button>
          <input
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            placeholder="Código de sala"
          />
          <button onClick={join}>Unirme a partida</button>
          {room && (
            <p>
              Sala {room.codigoSala} · {room.estado}
            </p>
          )}
        </section>
      )}
    </main>
  );
}
