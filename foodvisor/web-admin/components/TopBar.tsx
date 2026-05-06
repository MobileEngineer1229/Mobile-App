"use client";

import { LogOut } from "lucide-react";
import { useRouter } from "next/navigation";
import { clearSession, getUser } from "@/lib/auth";

export default function TopBar() {
  const router = useRouter();
  const user = getUser();

  function logout() {
    clearSession();
    router.replace("/login");
  }

  return (
    <header className="topbar">
      <div>
        <strong>Foodvisor Workspace</strong>
        <span>Nutrition content operations</span>
      </div>
      <div className="topbar-actions">
        <div>
          <strong>{user?.name || "Admin"}</strong>
          <span>{user?.email || ""}</span>
        </div>
        <button onClick={logout} type="button">
          <LogOut size={15} />
          Sign out
        </button>
      </div>
    </header>
  );
}
