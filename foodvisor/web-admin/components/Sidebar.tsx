"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { navItems } from "@/lib/navigation";

export default function Sidebar() {
  const pathname = usePathname();
  let lastGroup: string | null | undefined;

  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="brand-mark">F</span>
        <div>
          <strong>Foodvisor</strong>
          <small>Admin Panel</small>
        </div>
      </div>

      <nav className="nav">
        {navItems.map(({ group, href, label, icon: Icon }) => {
          const showGroup = group && group !== lastGroup;
          lastGroup = group;
          const active = pathname === href || (href !== "/" && pathname.startsWith(`${href}/`));

          return (
            <div key={href}>
              {showGroup ? <div className="nav-group">{group}</div> : null}
              <Link className={active ? "active" : ""} href={href}>
                <Icon size={17} />
                <span>{label}</span>
              </Link>
            </div>
          );
        })}
      </nav>
    </aside>
  );
}
